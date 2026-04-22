package dev.send.api.strategy.domain;

import javax.annotation.Nullable;

public record GraphEdge(
    String id,
    String source,
    String target,
    @Nullable String sourceHandle,
    @Nullable String targetHandle,
    @Nullable Integer sourcePort,
    @Nullable Integer targetPort) {}
