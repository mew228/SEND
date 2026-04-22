package dev.send.api.strategy.api.dto;

import java.time.Instant;
import java.util.List;

public record StoredStrategyDto(
        String id,
        String name,
        String kind,
        Instant updatedAt,
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges) {}
