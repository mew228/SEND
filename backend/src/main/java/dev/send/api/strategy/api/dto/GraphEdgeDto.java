package dev.send.api.strategy.api.dto;

import javax.annotation.Nullable;

public record GraphEdgeDto(
    String id,
    String source,
    String target,
    @Nullable String sourceHandle,
    @Nullable String targetHandle,
    @Nullable Integer sourcePort,
    @Nullable Integer targetPort) {}
