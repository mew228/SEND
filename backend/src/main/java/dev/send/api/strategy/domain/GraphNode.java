package dev.send.api.strategy.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record GraphNode(String id, String type, NodePosition position, JsonNode data) {}
