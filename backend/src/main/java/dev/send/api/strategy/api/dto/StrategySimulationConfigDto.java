package dev.send.api.strategy.api.dto;

public record StrategySimulationConfigDto(
    String startDate, String endDate, double initialCash, Boolean includeTrace) {}
