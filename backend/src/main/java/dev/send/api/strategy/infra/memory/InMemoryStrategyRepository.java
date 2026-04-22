package dev.send.api.strategy.infra.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.strategy.domain.StrategyRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStrategyRepository implements StrategyRepository {
  private static final Map<String, String> DEFAULT_STRATEGIES =
      Map.of(
          "logicex", "/seed-strategies/logicex.json",
          "aapl_buy_sell_template", "/seed-strategies/aapl_buy_sell_template.json");

  private final Map<String, StrategyDocument> store = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public InMemoryStrategyRepository(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    loadDefaultStrategies();
  }

  @Override
  public List<StrategyDocument> findAll() {
    return List.copyOf(store.values());
  }

  @Override
  public Optional<StrategyDocument> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public StrategyDocument save(StrategyDocument strategyDocument) {
    store.put(strategyDocument.id(), strategyDocument);
    return strategyDocument;
  }

  private void loadDefaultStrategies() {
    DEFAULT_STRATEGIES.forEach(
        (id, resourcePath) -> store.put(id, loadStrategyDocument(id, resourcePath)));
  }

  private StrategyDocument loadStrategyDocument(String id, String resourcePath) {
    try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IllegalStateException("Could not find strategy seed resource: " + resourcePath);
      }

      SeedStrategy seedStrategy =
          objectMapper.readValue(inputStream, new TypeReference<SeedStrategy>() {});
      return new StrategyDocument(
          id,
          seedStrategy.nodes().stream()
              .map(
                  node ->
                      new GraphNode(
                          node.id(),
                          node.type(),
                          new NodePosition(node.position().x(), node.position().y()),
                          normalizeNodeData(node.data())))
              .toList(),
          seedStrategy.edges().stream()
              .map(
                  edge ->
                      new GraphEdge(
                          edge.id(),
                          edge.source(),
                          edge.target(),
                          edge.sourceHandle(),
                          edge.targetHandle(),
                          null,
                          null))
              .toList());
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Could not load strategy seed resource: " + resourcePath, exception);
    }
  }

  private JsonNode normalizeNodeData(JsonNode data) {
    return data != null && data.isObject() ? data.deepCopy() : objectMapper.createObjectNode();
  }

  private record SeedStrategy(List<SeedNode> nodes, List<SeedEdge> edges) {}

  private record SeedNode(String id, String type, SeedPosition position, JsonNode data) {}

  private record SeedPosition(double x, double y) {}

  private record SeedEdge(
      String id, String source, String target, String sourceHandle, String targetHandle) {}
}
