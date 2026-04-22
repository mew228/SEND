package dev.send.api.lectures.api.dto;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import dev.send.api.strategy.api.dto.GraphEdgeDto;
import dev.send.api.strategy.api.dto.GraphNodeDto;
import dev.send.api.strategy.api.dto.NodePositionDto;

public final class LectureDtos {
    private LectureDtos() {}

    public record LectureCatalogPathDto(
            String slug,
            String title,
            @Nullable String description,
            List<LectureCatalogCategoryDto> categories) {}

    public record LectureCatalogCategoryDto(
            String slug,
            String title,
            @Nullable String description,
            @Nullable String hero,
            List<LectureCatalogItemDto> lectures) {}

    public record LectureCatalogItemDto(
            String id,
            String slug,
            String pathSlug,
            String categorySlug,
            String title,
            String summary,
            int estimatedMinutes) {}

    public record LectureCatalogResponseDto(
            List<LectureCatalogPathDto> paths) {}

    public record LecturePathDto(
            String slug,
            String title,
            @Nullable String description) {}

    public record LectureCategoryDto(
            String slug,
            String title,
            @Nullable String description,
            @Nullable String hero) {}

    public record LectureCheckpointDto(
            String id,
            String title,
            List<String> instructions,
            List<LectureCheckpointTaskDto> tasks,
            LectureSandboxPresetDto sandboxPreset,
            @Nullable LectureSimulationConfigDto simulationConfig) {}

    public record LectureSimulationConfigDto(
            double initialCash,
            @Nullable Boolean includeTrace) {}

    public record LectureCheckpointStateDto(
            @Nullable String lastFeedback,
            @Nullable String lastAttemptedAt,
            boolean passed) {}

    public record LectureCheckpointTaskDto(
            String id,
            String label,
            String description) {}

    public record LectureCheckpointVerifyRequestDto(
            List<GraphNodeDto> nodes,
            List<GraphEdgeDto> edges,
            @Nullable LectureSimulationConfigDto simulation) {}

    public record LectureCheckpointVerifyResponseDto(
            boolean passed,
            String feedback,
            int newlyUnlockedSublectureIndex,
            List<String> completedCheckpointIds,
            @Nullable LectureSublectureDto newlyUnlockedSublecture) {}

    public record LectureDetailResponseDto(
            String id,
            String slug,
            String pathSlug,
            String categorySlug,
            String title,
            String summary,
            int estimatedMinutes,
            LecturePathDto path,
            LectureCategoryDto category,
            List<LectureSublectureDto> sublectures,
            LectureProgressDto progress) {}

    public record LectureHeadingDto(
            String id,
            String title,
            int level) {}

    public record LectureProgressDto(
            String lectureId,
            int highestUnlockedSublectureIndex,
            List<String> completedCheckpointIds,
            Map<String, LectureCheckpointStateDto> activeCheckpointState) {}

    public record LectureProgressUpdateDto(
            String lectureId,
            int highestUnlockedSublectureIndex,
            List<String> completedCheckpointIds,
            Map<String, LectureCheckpointStateDto> activeCheckpointState) {}

    public record LectureSandboxEdgeDto(
            String id,
            String source,
            String target) {}

    public record LectureSandboxNodeDto(
            String id,
            String type,
            @Nullable String label,
            NodePositionDto position) {}

    public record LectureSandboxPresetDto(
            List<String> allowedNodeTypes,
            List<LectureSandboxNodeDto> starterNodes,
            List<LectureSandboxEdgeDto> starterEdges) {}

    public record LectureSublectureDto(
            String id,
            String title,
            @Nullable String contentSource,
            List<LectureHeadingDto> headings,
            @Nullable LectureCheckpointDto checkpointAfter,
            boolean unlocked) {}
}
