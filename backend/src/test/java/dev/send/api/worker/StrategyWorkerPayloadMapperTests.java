package dev.send.api.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.send.api.catalog.application.NodeCatalogService;
import dev.send.api.catalog.application.NodeSpecCatalogLoader;
import dev.send.api.strategy.application.StrategyGraphValidator;
import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.worker.application.StrategyWorkerPayloadMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyWorkerPayloadMapperTests {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final NodeCatalogService nodeCatalogService =
      new NodeCatalogService(new NodeSpecCatalogLoader(objectMapper));
  private final StrategyWorkerPayloadMapper strategyWorkerPayloadMapper =
      new StrategyWorkerPayloadMapper(
          objectMapper, new StrategyGraphValidator(nodeCatalogService), nodeCatalogService);

  @Test
  void normalizesFrontendGraphIntoWorkerPayload() {
    StrategyDocument strategyDocument =
        new StrategyDocument(
            "s-worker",
            List.of(
                new GraphNode(
                    "a",
                    "const_number",
                    new NodePosition(0, 0),
                    objectMapper.createObjectNode().put("value", 2)),
                new GraphNode(
                    "b",
                    "const_number",
                    new NodePosition(0, 100),
                    objectMapper.createObjectNode().put("value", 3)),
                new GraphNode(
                    "c", "add", new NodePosition(150, 50), objectMapper.createObjectNode())),
            List.of(
                new GraphEdge("e-1", "a", "c", "out:0", "in:0", null, null),
                new GraphEdge("e-2", "b", "c", "out:0", "in:1", null, null)));

    ObjectNode payload = strategyWorkerPayloadMapper.toWorkerPayload(strategyDocument);

    assertEquals("s-worker", payload.path("strategyId").asText());
    assertEquals(
        "const_number", payload.path("graph").path("nodes").get(0).path("nodeType").asText());
    assertEquals(
        "value",
        payload.path("graph").path("nodes").get(0).path("dataFields").get(0).path("name").asText());
    assertEquals(
        2,
        payload.path("graph").path("nodes").get(0).path("dataFields").get(0).path("value").asInt());
    assertEquals(0, payload.path("graph").path("edges").get(0).path("sourcePort").asInt());
    assertEquals(1, payload.path("graph").path("edges").get(1).path("targetPort").asInt());
    assertEquals(2, payload.path("nodeSpecs").size());
    assertTrue(payload.path("nodeSpecs").toString().contains("\"const_number\""));
    assertTrue(payload.path("nodeSpecs").toString().contains("\"add\""));
  }
}
