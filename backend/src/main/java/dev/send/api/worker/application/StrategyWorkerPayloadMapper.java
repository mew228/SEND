package dev.send.api.worker.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.send.api.catalog.application.NodeCatalogService;
import dev.send.api.catalog.domain.NodeSpec;
import dev.send.api.strategy.application.ResolvedGraphEdge;
import dev.send.api.strategy.application.StrategyGraphValidator;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.StrategyDocument;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Normalizes the stored frontend-oriented graph document into the OCaml worker payload shape. */
@Component
public class StrategyWorkerPayloadMapper {
  private final ObjectMapper objectMapper;
  private final StrategyGraphValidator strategyGraphValidator;
  private final NodeCatalogService nodeCatalogService;

  public StrategyWorkerPayloadMapper(
      ObjectMapper objectMapper,
      StrategyGraphValidator strategyGraphValidator,
      NodeCatalogService nodeCatalogService) {
    this.objectMapper = objectMapper;
    this.strategyGraphValidator = strategyGraphValidator;
    this.nodeCatalogService = nodeCatalogService;
  }

  public ObjectNode toWorkerPayload(StrategyDocument strategyDocument) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("strategyId", strategyDocument.id());
    addGraphPayload(payload, strategyDocument);
    return payload;
  }

  public ObjectNode toSimulationPayload(
      StrategyDocument strategyDocument, StrategySimulationConfig simulationConfig) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("strategyId", strategyDocument.id());
    addGraphPayload(payload, strategyDocument);

    ObjectNode simulation = payload.putObject("simulation");
    simulation.put("startDate", simulationConfig.startDate());
    simulation.put("endDate", simulationConfig.endDate());
    simulation.put("initialCash", simulationConfig.initialCash());
    simulation.put("includeTrace", simulationConfig.includeTrace());
    return payload;
  }

  private void addGraphPayload(ObjectNode payload, StrategyDocument strategyDocument) {
    ObjectNode graph = payload.putObject("graph");
    ArrayNode nodes = graph.putArray("nodes");
    for (GraphNode node : strategyDocument.nodes()) {
      ObjectNode outNode = nodes.addObject();
      outNode.put("id", node.id());
      outNode.put("nodeType", node.type());

      ArrayNode dataFields = outNode.putArray("dataFields");
      node.data().fields().forEachRemaining(entry -> dataFields.add(toDataField(entry)));
    }

    ArrayNode edges = graph.putArray("edges");
    List<ResolvedGraphEdge> resolvedEdges =
        strategyDocument.edges().stream()
            .map(edge -> strategyGraphValidator.resolveEdge(edge, strategyDocument.nodes()))
            .toList();
    for (ResolvedGraphEdge resolvedEdge : resolvedEdges) {
      ObjectNode outEdge = edges.addObject();
      outEdge.put("id", resolvedEdge.edge().id());
      outEdge.put("sourceNode", resolvedEdge.edge().source());
      outEdge.put("sourcePort", resolvedEdge.sourcePort());
      outEdge.put("targetNode", resolvedEdge.edge().target());
      outEdge.put("targetPort", resolvedEdge.targetPort());
    }

    ArrayNode nodeSpecs = payload.putArray("nodeSpecs");
    Set<String> referencedNodeTypes = new LinkedHashSet<>();
    for (GraphNode node : strategyDocument.nodes()) {
      referencedNodeTypes.add(node.type());
    }
    for (String nodeType : referencedNodeTypes) {
      NodeSpec spec =
          nodeCatalogService
              .findSpec(nodeType)
              .orElseThrow(
                  () -> new IllegalStateException("Missing node spec for node type: " + nodeType));
      nodeSpecs.add(spec.payload().deepCopy());
    }
  }

  private ObjectNode toDataField(Entry<String, JsonNode> entry) {
    ObjectNode field = objectMapper.createObjectNode();
    field.put("name", entry.getKey());
    field.set("value", entry.getValue().deepCopy());
    return field;
  }
}
