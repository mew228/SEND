package dev.send.api.strategy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record StrategySimulationResultDto(
    JsonNode summary,
    JsonNode portfolio,
    JsonNode finalNodeValues,
    List<JsonNode> trace,
    List<String> warnings) {}
