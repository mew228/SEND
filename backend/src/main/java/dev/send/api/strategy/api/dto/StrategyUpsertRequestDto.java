package dev.send.api.strategy.api.dto;

import java.util.List;

public record StrategyUpsertRequestDto(
        String name,
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges) {}
