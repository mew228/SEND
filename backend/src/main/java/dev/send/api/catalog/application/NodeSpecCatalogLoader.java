package dev.send.api.catalog.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.send.api.catalog.domain.NodeSpec;
import dev.send.api.catalog.domain.NodeSpecSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Scans and parses the shared node-spec JSON resources. */
@Component
public class NodeSpecCatalogLoader {
  private final ObjectMapper objectMapper;
  private final List<NodeSpecSetConfig> registeredSets;

  @Autowired
  public NodeSpecCatalogLoader(ObjectMapper objectMapper) {
    this(
        objectMapper,
        new NodeSpecSetConfig.Builder()
            .addSet(NodeSpecSet.PRIMITIVE, "node-specs/primitive")
            .addSet(NodeSpecSet.FETCH, "node-specs/fetch")
            .addSet(NodeSpecSet.DERIVED, "node-specs/derived")
            .build());
  }

  public NodeSpecCatalogLoader(ObjectMapper objectMapper, List<NodeSpecSetConfig> registeredSets) {
    this.objectMapper = objectMapper;
    this.registeredSets = List.copyOf(registeredSets);
  }

  public List<NodeSpecSetConfig> registeredSets() {
    return registeredSets;
  }

  public List<NodeSpec> loadAll() {
    List<NodeSpec> specs = new ArrayList<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    for (NodeSpecSetConfig config : registeredSets) {
      try {
        Resource[] resources =
            resolver.getResources("classpath*:" + config.resourceSubdirectory() + "/*.json");
        for (Resource resource : resources) {
          if (!resource.exists() || !resource.isReadable()) {
            continue;
          }
          specs.add(readNodeSpec(resource, config.set()));
        }
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to scan node-spec directory: " + config.resourceSubdirectory(), e);
      }
    }

    specs.sort(
        Comparator.comparing((NodeSpec spec) -> spec.set().name())
            .thenComparing(NodeSpec::nodeType));
    return List.copyOf(specs);
  }

  private NodeSpec readNodeSpec(Resource resource, NodeSpecSet defaultSet) {
    try (InputStream inputStream = resource.getInputStream()) {
      JsonNode payload = objectMapper.readTree(inputStream);
      String nodeType = payload.path("nodeType").asText("");
      if (nodeType.isBlank()) {
        throw new IllegalStateException(
            "Node spec is missing nodeType: " + resource.getDescription());
      }
      return new NodeSpec(nodeType, inferSet(payload, defaultSet, resource), payload);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read node spec: " + resource.getDescription(), e);
    }
  }

  private NodeSpecSet inferSet(JsonNode payload, NodeSpecSet defaultSet, Resource resource) {
    String rawSet = payload.path("set").asText("");
    if (rawSet.isBlank()) {
      return defaultSet;
    }

    try {
      return NodeSpecSet.valueOf(rawSet.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Unsupported node set '" + rawSet + "' in resource: " + resource.getDescription(), e);
    }
  }
}
