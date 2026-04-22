package dev.send.api.strategy.domain;

import java.util.List;

public record StrategyDocument(String id, List<GraphNode> nodes, List<GraphEdge> edges) {}
