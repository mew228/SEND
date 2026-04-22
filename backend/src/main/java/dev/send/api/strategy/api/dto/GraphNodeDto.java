package dev.send.api.strategy.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record GraphNodeDto(String id, String type, NodePositionDto position, JsonNode data) {}
