package dev.send.api.lectures.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import dev.send.api.lectures.api.dto.LectureDtos.LectureCheckpointVerifyResponseDto;
import dev.send.api.lectures.application.LectureSupport.LectureRuleEvaluationResult;
import dev.send.api.lectures.application.LectureSupport.LectureValidationException;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointState;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointSubmission;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;

@Service
public class LectureCheckpointVerificationService {
    private final LectureProgressService lectureProgressService;
    private final LectureRuleEvaluatorRegistry lectureRuleEvaluatorRegistry;
    private final LectureDtoMapper lectureDtoMapper;

    public LectureCheckpointVerificationService(
            LectureProgressService lectureProgressService,
            LectureRuleEvaluatorRegistry lectureRuleEvaluatorRegistry,
            LectureDtoMapper lectureDtoMapper) {
        this.lectureProgressService = lectureProgressService;
        this.lectureRuleEvaluatorRegistry = lectureRuleEvaluatorRegistry;
        this.lectureDtoMapper = lectureDtoMapper;
    }

    public LectureCheckpointVerifyResponseDto verify(
            LectureDefinition lecture,
            String checkpointId,
            LectureCheckpointSubmission submission,
            HttpServletRequest request,
            HttpServletResponse response) {
        LectureProgress currentProgress = lectureProgressService.getProgress(lecture, request);
        int checkpointIndex = resolveCheckpointIndex(lecture, checkpointId);

        if (currentProgress.completedCheckpointIds().contains(checkpointId)) {
            return new LectureCheckpointVerifyResponseDto(
                    true,
                    "Checkpoint already completed.",
                    currentProgress.highestUnlockedSublectureIndex(),
                    currentProgress.completedCheckpointIds(),
                    null);
        }

        if (checkpointIndex != currentProgress.highestUnlockedSublectureIndex()) {
            throw new LectureValidationException("Complete the current lecture checkpoint before attempting a later one.");
        }

        var checkpoint = lecture.sublectures().get(checkpointIndex).checkpointAfter();
        if (checkpoint == null) {
            throw new LectureValidationException("Checkpoint not found.");
        }

        LectureRuleEvaluationResult ruleResult = lectureRuleEvaluatorRegistry.evaluate(checkpoint.validation(), submission);
        Map<String, LectureCheckpointState> nextCheckpointState = new LinkedHashMap<>(currentProgress.activeCheckpointState());
        nextCheckpointState.put(checkpointId, new LectureCheckpointState(
                ruleResult.feedback(),
                Instant.now().toString(),
                ruleResult.passed()));

        if (!ruleResult.passed()) {
            LectureProgress failedProgress = lectureProgressService.saveTrustedProgress(
                    lecture,
                    new LectureProgress(
                            currentProgress.lectureId(),
                            currentProgress.highestUnlockedSublectureIndex(),
                            currentProgress.completedCheckpointIds(),
                            nextCheckpointState),
                    request,
                    response);
            return new LectureCheckpointVerifyResponseDto(
                    false,
                    ruleResult.feedback(),
                    failedProgress.highestUnlockedSublectureIndex(),
                    failedProgress.completedCheckpointIds(),
                    null);
        }

        int newlyUnlockedSublectureIndex = Math.min(checkpointIndex + 1, lecture.sublectures().size() - 1);
        List<String> completedCheckpointIds = new ArrayList<>(currentProgress.completedCheckpointIds());
        if (!completedCheckpointIds.contains(checkpointId)) {
            completedCheckpointIds.add(checkpointId);
        }

        LectureProgress passedProgress = lectureProgressService.saveTrustedProgress(
                lecture,
                new LectureProgress(
                        currentProgress.lectureId(),
                        Math.max(currentProgress.highestUnlockedSublectureIndex(), newlyUnlockedSublectureIndex),
                        completedCheckpointIds,
                        nextCheckpointState),
                request,
                response);

        return new LectureCheckpointVerifyResponseDto(
                true,
                ruleResult.feedback(),
                passedProgress.highestUnlockedSublectureIndex(),
                passedProgress.completedCheckpointIds(),
                passedProgress.highestUnlockedSublectureIndex() > checkpointIndex
                        ? lectureDtoMapper.toUnlockedSublectureDto(lecture, passedProgress.highestUnlockedSublectureIndex())
                        : null);
    }

    private int resolveCheckpointIndex(LectureDefinition lecture, String checkpointId) {
        for (int index = 0; index < lecture.sublectures().size(); index++) {
            var checkpoint = lecture.sublectures().get(index).checkpointAfter();
            if (checkpoint != null && Objects.requireNonNull(checkpoint).id().equals(checkpointId)) {
                return index;
            }
        }
        throw new LectureValidationException("Checkpoint not found.");
    }
}
