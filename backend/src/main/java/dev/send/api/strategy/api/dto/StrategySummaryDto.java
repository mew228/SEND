package dev.send.api.strategy.api.dto;

import java.time.Instant;

public record StrategySummaryDto(
        String id,
        String name,
        String kind,
        Instant updatedAt) {}
