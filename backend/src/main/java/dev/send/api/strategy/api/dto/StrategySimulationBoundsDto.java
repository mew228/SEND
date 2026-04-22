package dev.send.api.strategy.api.dto;

import javax.annotation.Nullable;

public record StrategySimulationBoundsDto(
    boolean hasPriceData, @Nullable String earliestPriceDate, @Nullable String latestPriceDate) {}
