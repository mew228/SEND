package dev.send.api.strategy.application;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.send.api.strategy.api.dto.GraphEdgeDto;
import dev.send.api.strategy.api.dto.GraphNodeDto;
import dev.send.api.strategy.api.dto.NodePositionDto;
import dev.send.api.strategy.api.dto.StoredStrategyDto;
import dev.send.api.strategy.api.dto.StrategySummaryDto;
import dev.send.api.strategy.api.dto.StrategyDocumentDto;
import dev.send.api.strategy.api.dto.StrategyUpsertRequestDto;
import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StoredStrategy;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.strategy.domain.StrategySummary;

@Component
public class StrategyDocumentMapper {
  private final ObjectMapper objectMapper;

  public StrategyDocumentMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

    public StrategyDocument toDomain(StrategyDocumentDto dto) {
        return toDomain(dto.id(), dto.nodes(), dto.edges());
    }

    public StrategyDocument toDomain(String id, StrategyUpsertRequestDto dto) {
        return toDomain(id, dto.nodes(), dto.edges());
    }

    public StrategyUpsertCommand toCommand(@Nullable String id, StrategyUpsertRequestDto dto) {
        return new StrategyUpsertCommand(
                id,
                dto.name(),
                toDomain(id == null ? "draft" : id, dto));
    }

    private StrategyDocument toDomain(String id, List<GraphNodeDto> nodesDto, List<GraphEdgeDto> edgesDto) {
        if (nodesDto == null) {
            throw new StrategyValidationException("Strategy nodes are required.");
        }
        if (edgesDto == null) {
            throw new StrategyValidationException("Strategy edges are required.");
        }

        List<GraphNode> nodes = nodesDto.stream()
                .map(node -> new GraphNode(
                        node.id(),
                        node.type(),
                        toDomain(node.position()),
                        normalizeData(node.data())))
                .toList();
        List<GraphEdge> edges = edgesDto.stream()
                .map(edge -> new GraphEdge(
                        edge.id(),
                        edge.source(),
                        edge.target(),
                        edge.sourceHandle(),
                        edge.targetHandle(),
                        edge.sourcePort(),
                        edge.targetPort()))
                .toList();
        return new StrategyDocument(id, nodes, edges);
    }

    public StoredStrategyDto toStoredDto(StoredStrategy strategy) {
        return new StoredStrategyDto(
                strategy.id(),
                strategy.name(),
                strategy.kind().name().toLowerCase(java.util.Locale.ROOT),
                strategy.updatedAt(),
                strategy.nodes().stream()
                        .map(node -> new GraphNodeDto(
                                node.id(),
                                node.type(),
                                toDto(node.position()),
                                node.data()))
                        .toList(),
                strategy.edges().stream()
                        .map(edge -> new GraphEdgeDto(
                                edge.id(),
                                edge.source(),
                                edge.target(),
                                edge.sourceHandle(),
                                edge.targetHandle(),
                                edge.sourcePort(),
                                edge.targetPort()))
                        .toList());
    }

    public StrategySummaryDto toSummaryDto(StrategySummary strategySummary) {
        return new StrategySummaryDto(
                strategySummary.id(),
                strategySummary.name(),
                strategySummary.kind().name().toLowerCase(java.util.Locale.ROOT),
                strategySummary.updatedAt());
    }

  public StrategyDocumentDto toDto(StrategyDocument strategyDocument) {
    return new StrategyDocumentDto(
        strategyDocument.id(),
        strategyDocument.nodes().stream()
            .map(
                node ->
                    new GraphNodeDto(node.id(), node.type(), toDto(node.position()), node.data()))
            .toList(),
        strategyDocument.edges().stream()
            .map(
                edge ->
                    new GraphEdgeDto(
                        edge.id(),
                        edge.source(),
                        edge.target(),
                        edge.sourceHandle(),
                        edge.targetHandle(),
                        edge.sourcePort(),
                        edge.targetPort()))
            .toList());
  }

  private NodePosition toDomain(NodePositionDto position) {
    if (position == null) {
      throw new StrategyValidationException("Node position is required.");
    }
    return new NodePosition(position.x(), position.y());
  }

  private NodePositionDto toDto(NodePosition position) {
    return new NodePositionDto(position.x(), position.y());
  }

  private JsonNode normalizeData(JsonNode data) {
    if (data != null && data.isObject()) {
      return data.deepCopy();
    }
    return objectMapper.createObjectNode();
  }
}
