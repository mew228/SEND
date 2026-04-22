package dev.send.api.strategy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.send.api.catalog.application.NodeCatalogService;
import dev.send.api.catalog.application.NodeSpecCatalogLoader;
import dev.send.api.strategy.application.StrategyGraphValidator;
import dev.send.api.strategy.application.StrategyValidationException;
import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StrategyDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyGraphValidatorTests {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final NodeCatalogService nodeCatalogService =
      new NodeCatalogService(new NodeSpecCatalogLoader(objectMapper));
  private final StrategyGraphValidator strategyGraphValidator =
      new StrategyGraphValidator(nodeCatalogService);

  @Test
  void acceptsTransportValidGraphWhenPortsAreUnambiguous() {
    StrategyDocument strategyDocument =
        new StrategyDocument(
            "s-1",
            List.of(
                new GraphNode(
                    "n-1",
                    "const_number",
                    new NodePosition(0, 0),
                    objectMapper.createObjectNode().put("value", 2)),
                new GraphNode(
                    "n-2", "to_string", new NodePosition(100, 0), objectMapper.createObjectNode())),
            List.of(new GraphEdge("e-1", "n-1", "n-2", null, null, null, null)));

    assertDoesNotThrow(() -> strategyGraphValidator.validate(strategyDocument));
    assertEquals(
        0,
        strategyGraphValidator
            .resolveEdge(strategyDocument.edges().getFirst(), strategyDocument.nodes())
            .sourcePort());
    assertEquals(
        0,
        strategyGraphValidator
            .resolveEdge(strategyDocument.edges().getFirst(), strategyDocument.nodes())
            .targetPort());
  }

  @Test
  void rejectsUnknownNodeType() {
    StrategyDocument strategyDocument =
        new StrategyDocument(
            "s-2",
            List.of(
                new GraphNode(
                    "n-1",
                    "does_not_exist",
                    new NodePosition(0, 0),
                    objectMapper.createObjectNode())),
            List.of());

    StrategyValidationException exception =
        assertThrows(
            StrategyValidationException.class,
            () -> strategyGraphValidator.validate(strategyDocument));

    assertEquals("Unknown node type: does_not_exist", exception.getMessage());
  }

  @Test
  void rejectsAmbiguousTargetPortWhenFrontendDoesNotSendHandles() {
    StrategyDocument strategyDocument =
        new StrategyDocument(
            "s-3",
            List.of(
                new GraphNode(
                    "a",
                    "const_number",
                    new NodePosition(0, 0),
                    objectMapper.createObjectNode().put("value", 1)),
                new GraphNode(
                    "b", "add", new NodePosition(100, 0), objectMapper.createObjectNode())),
            List.of(new GraphEdge("e-1", "a", "b", null, null, null, null)));

    StrategyValidationException exception =
        assertThrows(
            StrategyValidationException.class,
            () -> strategyGraphValidator.validate(strategyDocument));

    assertEquals(
        "Missing target port identity for node type add; send handle or port index.",
        exception.getMessage());
  }
}
