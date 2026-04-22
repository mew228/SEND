package dev.send.api.catalog.domain;

import com.fasterxml.jackson.databind.JsonNode;

/** Lightweight Java-side representation of one node specification loaded from JSON. */
public record NodeSpec(String nodeType, NodeSpecSet set, JsonNode payload) {}
