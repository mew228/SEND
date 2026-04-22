package dev.send.api.catalog.application;

import dev.send.api.catalog.domain.NodeSpecSet;
import java.util.ArrayList;
import java.util.List;

/** Declares one registered node-spec set and the resource directory it scans. */
public record NodeSpecSetConfig(NodeSpecSet set, String resourceSubdirectory) {

  public static final class Builder {
    private final List<NodeSpecSetConfig> configs = new ArrayList<>();

    public Builder addSet(NodeSpecSet set, String resourceSubdirectory) {
      configs.add(new NodeSpecSetConfig(set, resourceSubdirectory));
      return this;
    }

    public List<NodeSpecSetConfig> build() {
      return List.copyOf(configs);
    }
  }
}
