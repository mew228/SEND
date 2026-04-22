package dev.send.api.lectures.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.Nullable;

import dev.send.api.lectures.application.LectureSupport.LectureRuleEvaluationResult;
import dev.send.api.lectures.application.LectureSupport.LectureValidationException;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointRule;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointSubmission;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.worker.application.StrategyExecutionService;

@Component
public class LectureRuleEvaluatorRegistry {
    private final StrategyExecutionService strategyExecutionService;

    public LectureRuleEvaluatorRegistry(StrategyExecutionService strategyExecutionService) {
        this.strategyExecutionService = strategyExecutionService;
    }

    public LectureRuleEvaluationResult evaluate(
            List<LectureCheckpointRule> rules,
            LectureCheckpointSubmission submission) {
        CachedResults cachedResults = new CachedResults();

        for (LectureCheckpointRule rule : rules) {
            LectureRuleEvaluationResult result = evaluateRule(rule, submission, cachedResults);
            if (!result.passed()) {
                return result;
            }
        }

        return new LectureRuleEvaluationResult(true, "Checkpoint verified. The next sublecture is now unlocked.");
    }

    private LectureRuleEvaluationResult evaluateRule(
            LectureCheckpointRule rule,
            LectureCheckpointSubmission submission,
            CachedResults cachedResults) {
        return switch (rule.type()) {
            case "node_count" -> evaluateNodeCount(rule.payload(), submission);
            case "node_exists" -> evaluateNodeExists(rule.payload(), submission);
            case "node_field_equals" -> evaluateNodeFieldEquals(rule.payload(), submission);
            case "connection_exists" -> evaluateConnectionExists(rule.payload(), submission);
            case "worker_execute_output_equals" -> evaluateWorkerExecuteOutputEquals(rule.payload(), submission, cachedResults);
            case "worker_simulation_summary_equals" ->
                evaluateWorkerSimulationSummaryEquals(rule.payload(), submission, cachedResults);
            case "worker_simulation_summary_gte" -> evaluateWorkerSimulationSummaryGte(rule.payload(), submission, cachedResults);
            default -> throw new LectureValidationException("Unsupported lecture validation rule `" + rule.type() + "`.");
        };
    }

    private LectureRuleEvaluationResult evaluateNodeCount(JsonNode rule, LectureCheckpointSubmission submission) {
        String nodeType = textOrNull(rule, "nodeType");
        JsonNode fieldEquals = rule.path("fieldEquals");
        long matchingNodeCount = submission.strategy().nodes().stream()
                .filter(node -> nodeType == null || node.type().equals(nodeType))
                .filter(node -> fieldEquals.isObject() ? nodeMatchesFieldMap(node, fieldEquals) : true)
                .count();
        int expectedCount = rule.path("count").asInt(-1);
        if (expectedCount < 0) {
            throw new LectureValidationException("node_count rules require a non-negative count.");
        }
        if (matchingNodeCount == expectedCount) {
            return new LectureRuleEvaluationResult(true, "Node count requirement passed.");
        }
        String target = nodeType == null ? "nodes" : nodeType + " nodes";
        return new LectureRuleEvaluationResult(
                false,
                "Expected " + expectedCount + " " + target + " but found " + matchingNodeCount + ".");
    }

    private LectureRuleEvaluationResult evaluateNodeExists(JsonNode rule, LectureCheckpointSubmission submission) {
        String nodeType = requiredText(rule, "nodeType");
        boolean exists = submission.strategy().nodes().stream().anyMatch(node -> node.type().equals(nodeType));
        return exists
                ? new LectureRuleEvaluationResult(true, "Required node is present.")
                : new LectureRuleEvaluationResult(false, "Add a `" + nodeType + "` node before verifying.");
    }

    private LectureRuleEvaluationResult evaluateNodeFieldEquals(JsonNode rule, LectureCheckpointSubmission submission) {
        String nodeType = requiredText(rule, "nodeType");
        String field = requiredText(rule, "field");
        JsonNode expected = rule.get("expected");
        int count = rule.has("count") ? rule.path("count").asInt(1) : 1;
        long matchingCount = submission.strategy().nodes().stream()
                .filter(node -> node.type().equals(nodeType))
                .filter(node -> jsonEquals(node.data().get(field), expected))
                .count();
        if (matchingCount >= count) {
            return new LectureRuleEvaluationResult(true, "Required node field matches.");
        }
        return new LectureRuleEvaluationResult(
                false,
                "Update `" + nodeType + "` so `" + field + "` matches the expected value.");
    }

    private LectureRuleEvaluationResult evaluateConnectionExists(JsonNode rule, LectureCheckpointSubmission submission) {
        String sourceType = requiredText(rule, "sourceType");
        String targetType = requiredText(rule, "targetType");
        Map<String, String> nodeTypesById = submission.strategy().nodes().stream()
                .collect(java.util.stream.Collectors.toMap(GraphNode::id, GraphNode::type));

        boolean exists = submission.strategy().edges().stream().anyMatch(edge ->
                sourceType.equals(nodeTypesById.get(edge.source())) && targetType.equals(nodeTypesById.get(edge.target())));
        return exists
                ? new LectureRuleEvaluationResult(true, "Required connection exists.")
                : new LectureRuleEvaluationResult(false, "Connect `" + sourceType + "` to `" + targetType + "`.");
    }

    private LectureRuleEvaluationResult evaluateWorkerExecuteOutputEquals(
            JsonNode rule,
            LectureCheckpointSubmission submission,
            CachedResults cachedResults) {
        String nodeId = requiredText(rule, "nodeId");
        String output = requiredText(rule, "output");
        JsonNode expected = rule.get("expected");

        JsonNode executeResult = cachedResults.getExecuteResult(submission, strategyExecutionService);
        JsonNode actual = executeResult.path(nodeId).path(output);
        if (jsonEquals(actual, expected)) {
            return new LectureRuleEvaluationResult(true, "Worker execute output matches.");
        }
        return new LectureRuleEvaluationResult(
                false,
                "The graph output `" + nodeId + "." + output + "` did not match the expected value.");
    }

    private LectureRuleEvaluationResult evaluateWorkerSimulationSummaryEquals(
            JsonNode rule,
            LectureCheckpointSubmission submission,
            CachedResults cachedResults) {
        String field = requiredText(rule, "field");
        JsonNode expected = rule.get("expected");
        JsonNode summary = cachedResults.getSimulationSummary(submission, strategyExecutionService);
        JsonNode actual = summary.path(field);
        if (jsonEquals(actual, expected)) {
            return new LectureRuleEvaluationResult(true, "Worker simulation summary matches.");
        }
        return new LectureRuleEvaluationResult(
                false,
                "Simulation summary field `" + field + "` did not match the expected value.");
    }

    private LectureRuleEvaluationResult evaluateWorkerSimulationSummaryGte(
            JsonNode rule,
            LectureCheckpointSubmission submission,
            CachedResults cachedResults) {
        String field = requiredText(rule, "field");
        JsonNode expected = rule.get("expected");
        if (submission.simulationConfig() == null) {
            throw new LectureValidationException("Simulation-backed lecture rules require a simulation config.");
        }
        JsonNode summary = cachedResults.getSimulationSummary(submission, strategyExecutionService);
        JsonNode actual = summary.path(field);
        if (!actual.isNumber() || expected == null || !expected.isNumber()) {
            throw new LectureValidationException("worker_simulation_summary_gte requires numeric values.");
        }
        BigDecimal actualValue = actual.decimalValue();
        BigDecimal expectedValue = expected.decimalValue();
        if (actualValue.compareTo(expectedValue) >= 0) {
            return new LectureRuleEvaluationResult(true, "Simulation summary threshold passed.");
        }
        return new LectureRuleEvaluationResult(
                false,
                "Simulation summary field `" + field + "` must be at least " + expectedValue.toPlainString() + ".");
    }

    private boolean nodeMatchesFieldMap(GraphNode node, JsonNode fieldEquals) {
        return streamFields(fieldEquals).allMatch(entry -> jsonEquals(node.data().get(entry.getKey()), entry.getValue()));
    }

    private boolean jsonEquals(JsonNode actual, JsonNode expected) {
        if (actual == null || expected == null) {
            return actual == null && expected == null;
        }
        if (actual.isNumber() && expected.isNumber()) {
            return actual.decimalValue().compareTo(expected.decimalValue()) == 0;
        }
        return Objects.equals(actual, expected);
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = textOrNull(node, fieldName);
        if (value == null || value.isBlank()) {
            throw new LectureValidationException("Lecture rule field `" + fieldName + "` is required.");
        }
        return value;
    }

    private @Nullable String textOrNull(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.get(fieldName).asText().trim() : null;
    }

    private java.util.stream.Stream<Map.Entry<String, JsonNode>> streamFields(JsonNode objectNode) {
        List<Map.Entry<String, JsonNode>> entries = new java.util.ArrayList<>();
        objectNode.fields().forEachRemaining(entries::add);
        return entries.stream();
    }

    private static final class CachedResults {
        private @Nullable ObjectNode executeResult;
        private @Nullable ObjectNode simulationResult;

        private JsonNode getExecuteResult(
                LectureCheckpointSubmission submission,
                StrategyExecutionService strategyExecutionService) {
            if (executeResult == null) {
                executeResult = strategyExecutionService.executeGraphResults(submission.strategy());
            }
            return executeResult;
        }

        private JsonNode getSimulationSummary(
                LectureCheckpointSubmission submission,
                StrategyExecutionService strategyExecutionService) {
            if (submission.simulationConfig() == null) {
                throw new LectureValidationException("Simulation-backed lecture rules require a simulation config.");
            }
            if (simulationResult == null) {
                simulationResult = strategyExecutionService.simulateGraphResults(
                        submission.strategy(),
                        submission.simulationConfig());
            }
            return simulationResult.path("summary");
        }
    }
}
