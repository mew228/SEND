package dev.send.api.strategy.application;

import dev.send.api.strategy.domain.GraphEdge;

public record ResolvedGraphEdge(GraphEdge edge, int sourcePort, int targetPort) {}
