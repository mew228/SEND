package dev.send.api.worker.infra.ocaml;

import com.fasterxml.jackson.databind.JsonNode;

public record OcamlExecutionRequest(String command, JsonNode payload) {}
