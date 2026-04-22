package dev.send.api.strategy.application;

import javax.annotation.Nullable;

public record StrategySimulationBounds(
    boolean hasPriceData, @Nullable String earliestPriceDate, @Nullable String latestPriceDate) {}
