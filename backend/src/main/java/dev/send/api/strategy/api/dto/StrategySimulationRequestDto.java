package dev.send.api.strategy.api.dto;

public record StrategySimulationRequestDto(
    StrategyDocumentDto strategy, StrategySimulationConfigDto simulation) {}
