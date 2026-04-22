package dev.send.api.lectures.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.annotation.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.send.api.lectures.api.dto.LectureDtos.LectureCatalogResponseDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCheckpointVerifyRequestDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCheckpointVerifyResponseDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureDetailResponseDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureProgressDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureProgressUpdateDto;
import dev.send.api.lectures.application.LectureCheckpointVerificationService;
import dev.send.api.lectures.application.LectureDtoMapper;
import dev.send.api.lectures.application.LectureProgressService;
import dev.send.api.lectures.application.LectureService;
import dev.send.api.lectures.application.LectureSupport.LectureNotFoundException;
import dev.send.api.lectures.application.LectureSupport.LectureValidationException;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointSubmission;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.strategy.api.ApiErrorDto;
import dev.send.api.strategy.api.dto.StrategyDocumentDto;
import dev.send.api.strategy.application.StrategySimulationBoundsService;
import dev.send.api.strategy.application.StrategyDocumentMapper;
import dev.send.api.worker.application.StrategySimulationConfig;

@RestController
@RequestMapping("/api/lectures")
public class LectureController {
    private final LectureService lectureService;
    private final LectureDtoMapper lectureDtoMapper;
    private final LectureProgressService lectureProgressService;
    private final LectureCheckpointVerificationService lectureCheckpointVerificationService;
    private final StrategyDocumentMapper strategyDocumentMapper;
    private final StrategySimulationBoundsService strategySimulationBoundsService;

    public LectureController(
            LectureService lectureService,
            LectureDtoMapper lectureDtoMapper,
            LectureProgressService lectureProgressService,
            LectureCheckpointVerificationService lectureCheckpointVerificationService,
            StrategyDocumentMapper strategyDocumentMapper,
            StrategySimulationBoundsService strategySimulationBoundsService) {
        this.lectureService = lectureService;
        this.lectureDtoMapper = lectureDtoMapper;
        this.lectureProgressService = lectureProgressService;
        this.lectureCheckpointVerificationService = lectureCheckpointVerificationService;
        this.strategyDocumentMapper = strategyDocumentMapper;
        this.strategySimulationBoundsService = strategySimulationBoundsService;
    }

    @GetMapping
    public LectureCatalogResponseDto getLectureCatalog() {
        return lectureDtoMapper.toCatalogResponse(lectureService.findAll());
    }

    @GetMapping("/{pathSlug}/{categorySlug}/{lectureSlug}")
    public LectureDetailResponseDto getLecture(
            @PathVariable String pathSlug,
            @PathVariable String categorySlug,
            @PathVariable String lectureSlug,
            HttpServletRequest request) {
        LectureDefinition lecture = lectureService.getBySlug(pathSlug, categorySlug, lectureSlug);
        return lectureDtoMapper.toDetailDto(
                lecture,
                lectureProgressService.getProgress(lecture, request));
    }

    @GetMapping("/{lectureId}/progress")
    public LectureProgressDto getLectureProgress(
            @PathVariable String lectureId,
            HttpServletRequest request) {
        LectureDefinition lecture = lectureService.getById(lectureId);
        return lectureDtoMapper.toProgressDto(lectureProgressService.getProgress(lecture, request));
    }

    @PostMapping("/{lectureId}/progress")
    public LectureProgressDto saveLectureProgress(
            @PathVariable String lectureId,
            @RequestBody LectureProgressUpdateDto progressUpdateDto,
            HttpServletRequest request,
            HttpServletResponse response) {
        LectureDefinition lecture = lectureService.getById(lectureId);
        return lectureDtoMapper.toProgressDto(
                lectureProgressService.saveClientProgress(
                        lecture,
                        lectureDtoMapper.toDomain(new LectureProgressDto(
                                progressUpdateDto.lectureId(),
                                progressUpdateDto.highestUnlockedSublectureIndex(),
                                progressUpdateDto.completedCheckpointIds(),
                                progressUpdateDto.activeCheckpointState())),
                        request,
                        response));
    }

    @PostMapping("/{lectureId}/checkpoints/{checkpointId}/verify")
    public LectureCheckpointVerifyResponseDto verifyLectureCheckpoint(
            @PathVariable String lectureId,
            @PathVariable String checkpointId,
            @RequestBody LectureCheckpointVerifyRequestDto requestDto,
            HttpServletRequest request,
            HttpServletResponse response) {
        LectureDefinition lecture = lectureService.getById(lectureId);
        StrategyDocumentDto strategyDocumentDto = new StrategyDocumentDto(
                lectureId + "-" + checkpointId,
                requestDto.nodes(),
                requestDto.edges());
        StrategySimulationConfig simulationConfig = toSimulationConfig(requestDto.simulation());
        return lectureCheckpointVerificationService.verify(
                lecture,
                checkpointId,
                new LectureCheckpointSubmission(
                        strategyDocumentMapper.toDomain(strategyDocumentDto),
                        simulationConfig),
                request,
                response);
    }

    @ExceptionHandler(LectureValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorDto handleLectureValidationError(LectureValidationException exception) {
        String message = exception.getMessage();
        return new ApiErrorDto(
                "lecture_validation_failed",
                message == null ? "Lecture validation failed." : message,
                List.of());
    }

    @ExceptionHandler(LectureNotFoundException.class)
    public ResponseEntity<ApiErrorDto> handleLectureNotFound(LectureNotFoundException exception) {
        String message = exception.getMessage();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorDto(
                        "lecture_not_found",
                        message == null ? "Lecture not found." : message,
                        List.of()));
    }

    private @Nullable StrategySimulationConfig toSimulationConfig(
            @Nullable dev.send.api.lectures.api.dto.LectureDtos.LectureSimulationConfigDto simulationConfigDto) {
        if (simulationConfigDto == null) {
            return null;
        }
        return strategySimulationBoundsService.createLectureSimulationConfig(
                simulationConfigDto.initialCash(),
                simulationConfigDto.includeTrace() == null || simulationConfigDto.includeTrace());
    }
}
