package dev.send.api.strategy.application;

import javax.annotation.Nullable;

import dev.send.api.strategy.domain.StrategyDocument;

public record StrategyUpsertCommand(
        @Nullable String strategyId,
        String name,
        StrategyDocument document) {}
