package dev.send.api.lectures.domain;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.worker.application.StrategySimulationConfig;

public final class LectureModels {
    private LectureModels() {}

    public record LecturePath(
            String slug,
            String title,
            @Nullable String description) {}

    public record LectureCategory(
            String slug,
            String title,
            @Nullable String description,
            @Nullable String hero) {}

    public record LectureCheckpoint(
            String id,
            String title,
            List<String> instructions,
            List<LectureCheckpointTask> tasks,
            LectureSandboxPreset sandboxPreset,
            @Nullable LectureCheckpointSimulationConfig simulationConfig,
            List<LectureCheckpointRule> validation) {}

    public record LectureCheckpointSimulationConfig(
            double initialCash,
            boolean includeTrace) {}

    public record LectureCheckpointRule(
            String type,
            JsonNode payload) {}

    public record LectureCheckpointState(
            @Nullable String lastFeedback,
            @Nullable String lastAttemptedAt,
            boolean passed) {}

    public record LectureCheckpointSubmission(
            StrategyDocument strategy,
            @Nullable StrategySimulationConfig simulationConfig) {}

    public record LectureCheckpointTask(
            String id,
            String label,
            String description) {}

    public record LectureDefinition(
            String id,
            String slug,
            LecturePath path,
            LectureCategory category,
            String title,
            String summary,
            int estimatedMinutes,
            List<LectureSublecture> sublectures) {}

    public record LectureHeading(
            String id,
            String title,
            int level) {}

    public record LectureProgress(
            String lectureId,
            int highestUnlockedSublectureIndex,
            List<String> completedCheckpointIds,
            Map<String, LectureCheckpointState> activeCheckpointState) {}

    public record LectureSandboxEdge(
            String id,
            String source,
            String target) {}

    public record LectureSandboxNode(
            String id,
            String type,
            @Nullable String label,
            NodePosition position) {}

    public record LectureSandboxPreset(
            List<String> allowedNodeTypes,
            List<LectureSandboxNode> starterNodes,
            List<LectureSandboxEdge> starterEdges) {}

    public record LectureSublecture(
            String id,
            String title,
            String content,
            List<LectureHeading> headings,
            @Nullable LectureCheckpoint checkpointAfter) {}
}
