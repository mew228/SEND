package dev.send.api.worker.infra.ocaml;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import javax.annotation.Nullable;

public record OcamlExecutionResponse(
    String status,
    @Nullable String command,
    @Nullable JsonNode result,
    @Nullable String code,
    @Nullable String message,
    List<String> details) {}
