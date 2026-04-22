package dev.send.api.worker.application;

public record StrategySimulationConfig(
    String startDate, String endDate, double initialCash, boolean includeTrace) {}
