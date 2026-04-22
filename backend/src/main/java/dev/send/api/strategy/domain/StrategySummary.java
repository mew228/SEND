package dev.send.api.strategy.domain;

import java.time.Instant;

public record StrategySummary(
        String id,
        String name,
        StrategyKind kind,
        Instant updatedAt) {}
