package dev.send.api.strategy.domain;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nullable;

public record StoredStrategy(
        String id,
        String name,
        StrategyKind kind,
        @Nullable String ownerUserId,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        Instant updatedAt) {
    public StrategyDocument toDocument() {
        return new StrategyDocument(id, nodes, edges);
    }

    public StrategySummary toSummary() {
        return new StrategySummary(id, name, kind, updatedAt);
    }
}
