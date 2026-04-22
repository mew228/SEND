package dev.send.api.lectures.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

import dev.send.api.lectures.api.dto.LectureDtos.LectureCatalogCategoryDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCatalogItemDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCatalogPathDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCatalogResponseDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCategoryDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCheckpointDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCheckpointStateDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureCheckpointTaskDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureDetailResponseDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureHeadingDto;
import dev.send.api.lectures.api.dto.LectureDtos.LecturePathDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureProgressDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureSandboxEdgeDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureSandboxNodeDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureSandboxPresetDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureSimulationConfigDto;
import dev.send.api.lectures.api.dto.LectureDtos.LectureSublectureDto;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpoint;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureCategory;
import dev.send.api.lectures.domain.LectureModels.LecturePath;
import dev.send.api.lectures.domain.LectureModels.LectureProgress;
import dev.send.api.lectures.domain.LectureModels.LectureSublecture;
import dev.send.api.strategy.api.dto.NodePositionDto;
@Component
public class LectureDtoMapper {
    private static final List<CatalogPathSeed> CATALOG_SEEDS = List.of(
            new CatalogPathSeed(
                    new LecturePath("logic", "Logic", "Reasoning-focused lessons and graph-building fundamentals."),
                    List.of(
                            new LectureCategory(
                                    "getting-started",
                                    "Getting Started",
                                    null,
                                    null))),
            new CatalogPathSeed(
                    new LecturePath("economics", "Economics", "Market and economic intuition organized into guided lecture categories."),
                    List.of(
                            new LectureCategory(
                                    "getting-started",
                                    "Getting Started",
                                    null,
                                    null))));

    public LectureCatalogResponseDto toCatalogResponse(List<LectureDefinition> lectures) {
        Map<String, CatalogPathAccumulator> paths = new LinkedHashMap<>();

        for (CatalogPathSeed seed : CATALOG_SEEDS) {
            CatalogPathAccumulator pathAccumulator = new CatalogPathAccumulator(seed.path());
            for (LectureCategory category : seed.categories()) {
                pathAccumulator.categories().put(category.slug(), new CatalogCategoryAccumulator(category));
            }
            paths.put(seed.path().slug(), pathAccumulator);
        }

        for (LectureDefinition lecture : lectures) {
            CatalogPathAccumulator pathAccumulator = paths.computeIfAbsent(
                    lecture.path().slug(),
                    ignored -> new CatalogPathAccumulator(lecture.path()));
            CatalogCategoryAccumulator categoryAccumulator = pathAccumulator.categories().computeIfAbsent(
                    lecture.category().slug(),
                    ignored -> new CatalogCategoryAccumulator(lecture.category()));
            categoryAccumulator.lectures().add(toCatalogItem(lecture));
        }

        return new LectureCatalogResponseDto(paths.values().stream()
                .map(path -> new LectureCatalogPathDto(
                        path.path().slug(),
                        path.path().title(),
                        path.path().description(),
                        path.categories().values().stream()
                                .map(category -> new LectureCatalogCategoryDto(
                                        category.category().slug(),
                                        category.category().title(),
                                        category.category().description(),
                                        category.category().hero(),
                                        List.copyOf(category.lectures())))
                                .toList()))
                .toList());
    }

    public LectureDetailResponseDto toDetailDto(LectureDefinition lecture, LectureProgress progress) {
        return new LectureDetailResponseDto(
                lecture.id(),
                lecture.slug(),
                lecture.path().slug(),
                lecture.category().slug(),
                lecture.title(),
                lecture.summary(),
                lecture.estimatedMinutes(),
                new LecturePathDto(
                        lecture.path().slug(),
                        lecture.path().title(),
                        lecture.path().description()),
                new LectureCategoryDto(
                        lecture.category().slug(),
                        lecture.category().title(),
                        lecture.category().description(),
                        lecture.category().hero()),
                lecture.sublectures().stream()
                        .map(sublecture -> toSublectureDto(lecture, sublecture, progress))
                        .toList(),
                toProgressDto(progress));
    }

    public LectureSublectureDto toUnlockedSublectureDto(LectureDefinition lecture, int index) {
        LectureSublecture sublecture = lecture.sublectures().get(index);
        return new LectureSublectureDto(
                sublecture.id(),
                sublecture.title(),
                sublecture.content(),
                sublecture.headings().stream()
                        .map(heading -> new LectureHeadingDto(heading.id(), heading.title(), heading.level()))
                        .toList(),
                sublecture.checkpointAfter() == null ? null : toCheckpointDto(sublecture.checkpointAfter()),
                true);
    }

    public LectureProgressDto toProgressDto(LectureProgress progress) {
        return new LectureProgressDto(
                progress.lectureId(),
                progress.highestUnlockedSublectureIndex(),
                progress.completedCheckpointIds(),
                progress.activeCheckpointState().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> new LectureCheckpointStateDto(
                                        entry.getValue().lastFeedback(),
                                        entry.getValue().lastAttemptedAt(),
                                        entry.getValue().passed()),
                                (left, right) -> right,
                                LinkedHashMap::new)));
    }

    public LectureProgress toDomain(LectureProgressDto progressDto) {
        return new LectureProgress(
                progressDto.lectureId(),
                progressDto.highestUnlockedSublectureIndex(),
                progressDto.completedCheckpointIds() == null ? List.of() : progressDto.completedCheckpointIds(),
                progressDto.activeCheckpointState() == null
                        ? Map.of()
                        : progressDto.activeCheckpointState().entrySet().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> new dev.send.api.lectures.domain.LectureModels.LectureCheckpointState(
                                                entry.getValue().lastFeedback(),
                                                entry.getValue().lastAttemptedAt(),
                                                entry.getValue().passed()),
                                        (left, right) -> right,
                                        LinkedHashMap::new)));
    }

    private LectureCatalogItemDto toCatalogItem(LectureDefinition lecture) {
        return new LectureCatalogItemDto(
                lecture.id(),
                lecture.slug(),
                lecture.path().slug(),
                lecture.category().slug(),
                lecture.title(),
                lecture.summary(),
                lecture.estimatedMinutes());
    }

    private LectureSublectureDto toSublectureDto(
            LectureDefinition lecture,
            LectureSublecture sublecture,
            LectureProgress progress) {
        int sublectureIndex = lecture.sublectures().indexOf(sublecture);
        boolean unlocked = sublectureIndex <= progress.highestUnlockedSublectureIndex();
        return new LectureSublectureDto(
                sublecture.id(),
                sublecture.title(),
                unlocked ? sublecture.content() : null,
                sublecture.headings().stream()
                        .map(heading -> new LectureHeadingDto(heading.id(), heading.title(), heading.level()))
                        .toList(),
                unlocked && sublecture.checkpointAfter() != null ? toCheckpointDto(sublecture.checkpointAfter()) : null,
                unlocked);
    }

    private LectureCheckpointDto toCheckpointDto(LectureCheckpoint checkpoint) {
        return new LectureCheckpointDto(
                checkpoint.id(),
                checkpoint.title(),
                checkpoint.instructions(),
                checkpoint.tasks().stream()
                        .map(task -> new LectureCheckpointTaskDto(task.id(), task.label(), task.description()))
                        .toList(),
                new LectureSandboxPresetDto(
                        checkpoint.sandboxPreset().allowedNodeTypes(),
                        checkpoint.sandboxPreset().starterNodes().stream()
                                .map(node -> new LectureSandboxNodeDto(
                                        node.id(),
                                        node.type(),
                                        node.label(),
                                        new NodePositionDto(node.position().x(), node.position().y())))
                                .toList(),
                        checkpoint.sandboxPreset().starterEdges().stream()
                                .map(edge -> new LectureSandboxEdgeDto(edge.id(), edge.source(), edge.target()))
                                .toList()),
                checkpoint.simulationConfig() == null
                        ? null
                        : new LectureSimulationConfigDto(
                                checkpoint.simulationConfig().initialCash(),
                                checkpoint.simulationConfig().includeTrace()));
    }

    private record CatalogPathSeed(
            LecturePath path,
            List<LectureCategory> categories) {}

    private record CatalogPathAccumulator(
            LecturePath path,
            Map<String, CatalogCategoryAccumulator> categories) {
        private CatalogPathAccumulator(LecturePath path) {
            this(path, new LinkedHashMap<>());
        }
    }

    private record CatalogCategoryAccumulator(
            LectureCategory category,
            List<LectureCatalogItemDto> lectures) {
        private CatalogCategoryAccumulator(LectureCategory category) {
            this(category, new ArrayList<>());
        }
    }
}
