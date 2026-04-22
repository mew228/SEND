package dev.send.api.strategy.application;

import com.fasterxml.jackson.databind.JsonNode;
import dev.send.api.catalog.application.NodeCatalogService;
import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.StrategyDocument;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class StrategyGraphValidator {
    private static final Pattern HANDLE_PATTERN = Pattern.compile("^(?:in|out):(\\d+)$");
    private static final int MAX_NODE_COUNT = 250;
    private static final int MAX_EDGE_COUNT = 500;
    private static final int MAX_IDENTIFIER_LENGTH = 120;
    private static final int MAX_NODE_TYPE_LENGTH = 80;
    private static final int MAX_DATA_STRING_LENGTH = 256;

    private final NodeCatalogService nodeCatalogService;

    public StrategyGraphValidator(NodeCatalogService nodeCatalogService) {
        this.nodeCatalogService = nodeCatalogService;
    }

    public void validate(StrategyDocument strategyDocument) {
        if (strategyDocument.id() == null || strategyDocument.id().isBlank()) {
            throw new StrategyValidationException("Strategy id is required.");
        }
        validateTextLength(strategyDocument.id(), "Strategy id", MAX_IDENTIFIER_LENGTH);
        if (strategyDocument.nodes() == null) {
            throw new StrategyValidationException("Strategy nodes are required.");
        }
        if (strategyDocument.edges() == null) {
            throw new StrategyValidationException("Strategy edges are required.");
        }
        if (strategyDocument.nodes().size() > MAX_NODE_COUNT) {
            throw new StrategyValidationException(
                    "Strategies are limited to " + MAX_NODE_COUNT + " nodes per request.");
        }
        if (strategyDocument.edges().size() > MAX_EDGE_COUNT) {
            throw new StrategyValidationException(
                    "Strategies are limited to " + MAX_EDGE_COUNT + " edges per request.");
        }

        Set<String> nodeIds = new HashSet<>();
        for (GraphNode node : strategyDocument.nodes()) {
            validateNode(node, nodeIds);
        }

        for (GraphEdge edge : strategyDocument.edges()) {
            resolveEdge(edge, strategyDocument.nodes());
        }
    }

    public ResolvedGraphEdge resolveEdge(GraphEdge edge, List<GraphNode> nodes) {
        if (edge.id() == null || edge.id().isBlank()) {
            throw new StrategyValidationException("Edge id is required.");
        }
        validateTextLength(edge.id(), "Edge id", MAX_IDENTIFIER_LENGTH);
        validateTextLength(edge.source(), "Edge source node id", MAX_IDENTIFIER_LENGTH);
        validateTextLength(edge.target(), "Edge target node id", MAX_IDENTIFIER_LENGTH);

        GraphNode sourceNode = findNode(nodes, edge.source(), "source");
        GraphNode targetNode = findNode(nodes, edge.target(), "target");

        int sourcePort = resolvePort(edge.sourcePort(), edge.sourceHandle(), sourceNode.type(), true);
        int targetPort = resolvePort(edge.targetPort(), edge.targetHandle(), targetNode.type(), false);

        findPort(nodeCatalogService.outputPorts(sourceNode.type()), sourcePort, "source");
        findPort(nodeCatalogService.inputPorts(targetNode.type()), targetPort, "target");

        return new ResolvedGraphEdge(edge, sourcePort, targetPort);
    }

    private void validateNode(GraphNode node, Set<String> nodeIds) {
        if (node.id() == null || node.id().isBlank()) {
            throw new StrategyValidationException("Node id is required.");
        }
        validateTextLength(node.id(), "Node id", MAX_IDENTIFIER_LENGTH);
        if (!nodeIds.add(node.id())) {
            throw new StrategyValidationException("Duplicate node id: " + node.id());
        }
        if (node.type() == null || node.type().isBlank()) {
            throw new StrategyValidationException("Node type is required for node: " + node.id());
        }
        validateTextLength(node.type(), "Node type", MAX_NODE_TYPE_LENGTH);
        if (node.position() == null) {
            throw new StrategyValidationException("Node position is required for node: " + node.id());
        }
        if (!Double.isFinite(node.position().x()) || !Double.isFinite(node.position().y())) {
            throw new StrategyValidationException("Node position must be finite for node: " + node.id());
        }

        if (nodeCatalogService.findSpec(node.type()).isEmpty()) {
            throw new StrategyValidationException("Unknown node type: " + node.type());
        }

        if (node.data() == null || !node.data().isObject()) {
            throw new StrategyValidationException("Node data must be an object for node: " + node.id());
        }

        JsonNode dataFields = nodeCatalogService.dataFields(node.type());
        if (!dataFields.isArray()) {
            throw new StrategyValidationException(
                    "Node spec data fields are malformed for node: " + node.id());
        }

        Set<String> allowedFields = new HashSet<>();
        for (JsonNode dataFieldSpec : dataFields) {
            String fieldName = dataFieldSpec.path("name").asText("");
            if (!fieldName.isBlank()) {
                allowedFields.add(fieldName);
            }
        }

        node.data().fields().forEachRemaining(entry -> {
            if (!allowedFields.contains(entry.getKey())) {
                throw new StrategyValidationException(
                        "Unknown data field '" + entry.getKey() + "' for node: " + node.id());
            }
            validateDataFieldValue(node.id(), entry.getKey(), entry.getValue());
        });
    }

    private GraphNode findNode(List<GraphNode> nodes, String nodeId, String role) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new StrategyValidationException("Edge " + role + " node id is required.");
        }

        return nodes.stream()
                .filter(node -> node.id().equals(nodeId))
                .findFirst()
                .orElseThrow(
                        () -> new StrategyValidationException("Unknown " + role + " node id: " + nodeId));
    }

    private int resolvePort(@Nullable Integer explicitPort, @Nullable String handle, String nodeType, boolean source) {
        if (explicitPort != null) {
            return explicitPort;
        }

        Integer handlePort = parseHandle(handle);
        if (handlePort != null) {
            return handlePort;
        }

        List<JsonNode> ports = source ? nodeCatalogService.outputPorts(nodeType)
                : nodeCatalogService.inputPorts(nodeType);
        if (ports.size() == 1) {
            return ports.get(0).path("index").asInt(-1);
        }

        String direction = source ? "source" : "target";
        throw new StrategyValidationException(
                "Missing "
                        + direction
                        + " port identity for node type "
                        + nodeType
                        + "; send handle or port index.");
    }

    @Nullable
    private Integer parseHandle(@Nullable String handle) {
        if (handle == null || handle.isBlank()) {
            return null;
        }
        validateTextLength(handle, "Edge handle", 16);

        Matcher matcher = HANDLE_PATTERN.matcher(handle);
        if (!matcher.matches()) {
            throw new StrategyValidationException("Unsupported edge handle format: " + handle);
        }
        return Integer.valueOf(matcher.group(1));
    }

    private JsonNode findPort(List<JsonNode> ports, int portIndex, String direction) {
        return ports.stream()
                .filter(port -> port.path("index").asInt(Integer.MIN_VALUE) == portIndex)
                .findFirst()
                .orElseThrow(
                        () -> new StrategyValidationException(
                                "Invalid " + direction + " port index: " + portIndex));
    }

    private void validateDataFieldValue(String nodeId, String fieldName, JsonNode value) {
        if (value == null || value.isNull() || value.isBoolean() || value.isNumber()) {
            return;
        }
        if (value.isTextual()) {
            validateTextLength(
                    value.asText(),
                    "Data field '" + fieldName + "' for node " + nodeId,
                    MAX_DATA_STRING_LENGTH);
            return;
        }

        throw new StrategyValidationException(
                "Data field '" + fieldName + "' for node " + nodeId + " must be a scalar value.");
    }

    private void validateTextLength(@Nullable String value, String fieldLabel, int maxLength) {
        if (value == null) {
            return;
        }
        if (value.length() > maxLength) {
            throw new StrategyValidationException(
                    fieldLabel + " exceeds the " + maxLength + "-character limit.");
        }
    }
}
