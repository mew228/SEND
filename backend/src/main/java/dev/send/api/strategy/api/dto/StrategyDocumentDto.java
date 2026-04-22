package dev.send.api.strategy.api.dto;

import java.util.List;

public record StrategyDocumentDto(String id, List<GraphNodeDto> nodes, List<GraphEdgeDto> edges) {}
