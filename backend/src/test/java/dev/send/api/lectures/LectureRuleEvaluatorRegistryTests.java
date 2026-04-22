package dev.send.api.lectures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.send.api.lectures.application.LectureRuleEvaluatorRegistry;
import dev.send.api.lectures.application.LectureSupport.LectureRuleEvaluationResult;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointRule;
import dev.send.api.lectures.domain.LectureModels.LectureCheckpointSubmission;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.worker.application.StrategyExecutionService;
import dev.send.api.worker.application.StrategySimulationConfig;

class LectureRuleEvaluatorRegistryTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void evaluatesStructuralRulesAgainstGraphPayload() {
        StrategyExecutionService strategyExecutionService = Mockito.mock(StrategyExecutionService.class);
        LectureRuleEvaluatorRegistry registry = new LectureRuleEvaluatorRegistry(strategyExecutionService);

        LectureCheckpointSubmission submission = new LectureCheckpointSubmission(
                new StrategyDocument(
                        "lecture-checkpoint",
                        List.of(
                                new GraphNode("n-if", "if", new NodePosition(0, 0), objectMapper.createObjectNode()),
                                new GraphNode("n-buy", "buy", new NodePosition(1, 1), objectMapper.createObjectNode())),
                        List.of(new dev.send.api.strategy.domain.GraphEdge("e-1", "n-if", "n-buy", null, null, null, null))),
                null);

        LectureRuleEvaluationResult result = registry.evaluate(
                List.of(
                        new LectureCheckpointRule("node_exists", objectMapper.createObjectNode().put("type", "node_exists").put("nodeType", "if")),
                        new LectureCheckpointRule(
                                "connection_exists",
                                objectMapper.createObjectNode()
                                        .put("type", "connection_exists")
                                        .put("sourceType", "if")
                                        .put("targetType", "buy"))),
                submission);

        assertTrue(result.passed());
    }

    @Test
    void evaluatesWorkerBackedRulesThroughStrategyExecutionService() {
        StrategyExecutionService strategyExecutionService = Mockito.mock(StrategyExecutionService.class);
        LectureRuleEvaluatorRegistry registry = new LectureRuleEvaluatorRegistry(strategyExecutionService);

        ObjectNode executeResult = objectMapper.createObjectNode();
        executeResult.putObject("buy-1").put("executed", true);
        when(strategyExecutionService.executeGraphResults(any(StrategyDocument.class))).thenReturn(executeResult);

        ObjectNode simulationResult = objectMapper.createObjectNode();
        simulationResult.putObject("summary").put("finalEquity", 1250.0);
        when(strategyExecutionService.simulateGraphResults(any(StrategyDocument.class), any(StrategySimulationConfig.class)))
                .thenReturn(simulationResult);

        LectureCheckpointSubmission submission = new LectureCheckpointSubmission(
                new StrategyDocument("lecture-worker", List.of(), List.of()),
                new StrategySimulationConfig("2020-01-01", "2020-01-31", 1000, true));

        LectureRuleEvaluationResult successResult = registry.evaluate(
                List.of(
                        new LectureCheckpointRule(
                                "worker_execute_output_equals",
                                objectMapper.createObjectNode()
                                        .put("type", "worker_execute_output_equals")
                                        .put("nodeId", "buy-1")
                                        .put("output", "executed")
                                        .put("expected", true)),
                        new LectureCheckpointRule(
                                "worker_simulation_summary_gte",
                                objectMapper.createObjectNode()
                                        .put("type", "worker_simulation_summary_gte")
                                        .put("field", "finalEquity")
                                        .put("expected", 1200.0))),
                submission);

        assertTrue(successResult.passed());

        LectureRuleEvaluationResult failureResult = registry.evaluate(
                List.of(
                        new LectureCheckpointRule(
                                "worker_simulation_summary_equals",
                                objectMapper.createObjectNode()
                                        .put("type", "worker_simulation_summary_equals")
                                        .put("field", "finalEquity")
                                        .put("expected", 1400.0))),
                submission);

        assertFalse(failureResult.passed());
    }
}
