package dev.send.api.lectures.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;

import dev.send.api.lectures.application.LectureSupport.LectureValidationException;
import dev.send.api.lectures.domain.LectureModels.LectureCategory;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpoint;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointSimulationConfig;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointRule;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointTask;
import dev.send.api.lectures.domain.LectureModels.LectureDefinition;
import dev.send.api.lectures.domain.LectureModels.LectureHeading;
import dev.send.api.lectures.domain.LectureModels.LecturePath;
import dev.send.api.lectures.domain.LectureModels.LectureSandboxEdge;
import dev.send.api.lectures.domain.LectureModels.LectureSandboxNode;
import dev.send.api.lectures.domain.LectureModels.LectureSandboxPreset;
import dev.send.api.lectures.domain.LectureModels.LectureSublecture;
import dev.send.api.strategy.domain.NodePosition;
@Component
public class LectureMarkdownParser {
    private static final String LECTURE_MARKER = ":::lecture ";
    private static final String SUBLECTURE_MARKER = ":::sublecture ";
    private static final String CHECKPOINT_MARKER = ":::checkpoint";
    private static final String CHECKPOINT_END_MARKER = ":::";

    private final ObjectMapper objectMapper;

    public LectureMarkdownParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LectureDefinition parse(String markdownSource) {
        List<String> lines = List.of(markdownSource.split("\\R", -1));
        JsonNode lectureMetadata = null;
        SublectureBuilder currentSublecture = null;
        List<LectureSublecture> parsedSublectures = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String trimmed = line.trim();

            if (trimmed.startsWith(LECTURE_MARKER)) {
                lectureMetadata = parseJson(trimmed.substring(LECTURE_MARKER.length()).trim());
                continue;
            }

            if (trimmed.startsWith(SUBLECTURE_MARKER)) {
                if (lectureMetadata == null) {
                    throw new LectureValidationException("Lecture metadata marker must appear before sublectures.");
                }
                if (currentSublecture != null) {
                    parsedSublectures.add(currentSublecture.build(parsedSublectures.size()));
                }
                JsonNode sublectureMetadata = parseJson(trimmed.substring(SUBLECTURE_MARKER.length()).trim());
                currentSublecture = new SublectureBuilder(
                        requiredText(sublectureMetadata, "id"),
                        requiredText(sublectureMetadata, "title"));
                continue;
            }

            if (trimmed.equals(CHECKPOINT_MARKER)) {
                if (currentSublecture == null) {
                    throw new LectureValidationException("Checkpoint marker must appear after a sublecture marker.");
                }
                List<String> jsonLines = new ArrayList<>();
                index += 1;
                while (index < lines.size() && !lines.get(index).trim().equals(CHECKPOINT_END_MARKER)) {
                    jsonLines.add(lines.get(index));
                    index += 1;
                }
                if (index >= lines.size()) {
                    throw new LectureValidationException("Checkpoint block is missing its closing marker.");
                }
                currentSublecture.setCheckpoint(parseCheckpoint(String.join("\n", jsonLines)));
                continue;
            }

            if (currentSublecture != null) {
                currentSublecture.addContentLine(line);
            }
        }

        if (lectureMetadata == null) {
            throw new LectureValidationException("Lecture markdown is missing a :::lecture marker.");
        }
        if (currentSublecture != null) {
            parsedSublectures.add(currentSublecture.build(parsedSublectures.size()));
        }
        if (parsedSublectures.isEmpty()) {
            throw new LectureValidationException("Lecture markdown must contain at least one sublecture.");
        }

        JsonNode pathNode = lectureMetadata.path("path");
        LecturePath path = new LecturePath(
                requiredText(pathNode, "slug"),
                requiredText(pathNode, "title"),
                optionalText(pathNode, "description"));

        JsonNode categoryNode = lectureMetadata.path("category");
        LectureCategory category = new LectureCategory(
                requiredText(categoryNode, "slug"),
                requiredText(categoryNode, "title"),
                optionalText(categoryNode, "description"),
                optionalText(categoryNode, "hero"));

        return new LectureDefinition(
                requiredText(lectureMetadata, "id"),
                requiredText(lectureMetadata, "slug"),
                path,
                category,
                requiredText(lectureMetadata, "title"),
                requiredText(lectureMetadata, "summary"),
                lectureMetadata.path("estimatedMinutes").asInt(0),
                List.copyOf(parsedSublectures));
    }

    private LectureCheckpoint parseCheckpoint(String rawJson) {
        JsonNode checkpointNode = parseJson(rawJson);

        return new LectureCheckpoint(
                requiredText(checkpointNode, "id"),
                requiredText(checkpointNode, "title"),
                toStringList(checkpointNode.path("instructions")),
                toTasks(checkpointNode.path("tasks")),
                toSandboxPreset(checkpointNode.path("sandboxPreset")),
                toSimulationConfig(checkpointNode.path("simulationConfig")),
                toValidationRules(checkpointNode.path("validation")));
    }

    private @Nullable LectureCheckpointSimulationConfig toSimulationConfig(JsonNode simulationConfigNode) {
        if (simulationConfigNode == null || simulationConfigNode.isMissingNode() || simulationConfigNode.isNull()) {
            return null;
        }

        return new LectureCheckpointSimulationConfig(
                simulationConfigNode.path("initialCash").asDouble(),
                !simulationConfigNode.has("includeTrace") || simulationConfigNode.path("includeTrace").asBoolean(true));
    }

    private LectureSandboxPreset toSandboxPreset(JsonNode sandboxPresetNode) {
        return new LectureSandboxPreset(
                toStringList(sandboxPresetNode.path("allowedNodeTypes")),
                toSandboxNodes(sandboxPresetNode.path("starterNodes")),
                toSandboxEdges(sandboxPresetNode.path("starterEdges")));
    }

    private List<LectureSandboxNode> toSandboxNodes(JsonNode rawNodes) {
        if (!rawNodes.isArray()) {
            return List.of();
        }

        List<LectureSandboxNode> nodes = new ArrayList<>();
        for (JsonNode rawNode : rawNodes) {
            JsonNode position = rawNode.path("position");
            nodes.add(new LectureSandboxNode(
                    requiredText(rawNode, "id"),
                    requiredText(rawNode, "type"),
                    optionalText(rawNode, "label"),
                    new NodePosition(position.path("x").asDouble(), position.path("y").asDouble())));
        }
        return List.copyOf(nodes);
    }

    private List<LectureSandboxEdge> toSandboxEdges(JsonNode rawEdges) {
        if (!rawEdges.isArray()) {
            return List.of();
        }

        List<LectureSandboxEdge> edges = new ArrayList<>();
        for (JsonNode rawEdge : rawEdges) {
            edges.add(new LectureSandboxEdge(
                    requiredText(rawEdge, "id"),
                    requiredText(rawEdge, "source"),
                    requiredText(rawEdge, "target")));
        }
        return List.copyOf(edges);
    }

    private List<LectureCheckpointTask> toTasks(JsonNode rawTasks) {
        if (!rawTasks.isArray()) {
            return List.of();
        }

        List<LectureCheckpointTask> tasks = new ArrayList<>();
        for (JsonNode rawTask : rawTasks) {
            tasks.add(new LectureCheckpointTask(
                    requiredText(rawTask, "id"),
                    requiredText(rawTask, "label"),
                    requiredText(rawTask, "description")));
        }
        return List.copyOf(tasks);
    }

    private List<LectureCheckpointRule> toValidationRules(JsonNode rawValidation) {
        if (!rawValidation.isArray()) {
            return List.of();
        }

        List<LectureCheckpointRule> rules = new ArrayList<>();
        for (JsonNode rawRule : rawValidation) {
            rules.add(new LectureCheckpointRule(
                    requiredText(rawRule, "type"),
                    rawRule.deepCopy()));
        }
        return List.copyOf(rules);
    }

    private List<String> toStringList(JsonNode rawValue) {
        if (!rawValue.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : rawValue) {
            if (item.isTextual()) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }

    private JsonNode parseJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception exception) {
            throw new LectureValidationException("Failed to parse lecture markdown JSON marker.");
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node, fieldName);
        if (value == null || value.isBlank()) {
            throw new LectureValidationException("Lecture markdown is missing required field `" + fieldName + "`.");
        }
        return value;
    }

    private @Nullable String optionalText(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.get(fieldName).asText() : null;
    }

    private static final class SublectureBuilder {
        private final String id;
        private final String title;
        private final List<String> contentLines = new ArrayList<>();
        private @Nullable LectureCheckpoint checkpoint;

        private SublectureBuilder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        private void addContentLine(String line) {
            contentLines.add(line);
        }

        private void setCheckpoint(LectureCheckpoint checkpoint) {
            this.checkpoint = checkpoint;
        }

        private LectureSublecture build(int index) {
            String content = String.join("\n", contentLines).trim();
            return new LectureSublecture(
                    id,
                    title,
                    content,
                    extractHeadings(index, title, content),
                    checkpoint);
        }

        private List<LectureHeading> extractHeadings(int index, String sublectureTitle, String content) {
            List<LectureHeading> headings = new ArrayList<>();
            String sectionId = "sublecture-" + index + "-" + slugify(sublectureTitle);
            for (String line : content.lines().toList()) {
                String trimmed = line.trim();
                if (trimmed.startsWith("## ")) {
                    String headingTitle = trimmed.substring(3).trim();
                    headings.add(new LectureHeading(
                            sectionId + "--" + slugify(headingTitle),
                            headingTitle,
                            2));
                }
            }
            return List.copyOf(headings);
        }

        private String slugify(String rawValue) {
            String normalized = rawValue.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s-]", "").trim();
            return normalized.replaceAll("\\s+", "-").replaceAll("-+", "-");
        }
    }
}
