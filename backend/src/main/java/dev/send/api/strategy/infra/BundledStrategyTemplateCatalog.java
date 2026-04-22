package dev.send.api.strategy.infra;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.NodePosition;
import dev.send.api.strategy.domain.StoredStrategy;
import dev.send.api.strategy.domain.StrategyKind;

@Component
public class BundledStrategyTemplateCatalog {
    private static final Map<String, TemplateSeed> DEFAULT_STRATEGIES = Map.of(
            "logicex", new TemplateSeed("/seed-strategies/logicex.json", "LogicEx"),
            "aapl_buy_sell_template", new TemplateSeed("/seed-strategies/aapl_buy_sell_template.json", "AAPL Buy/Sell Template"));

    private final Map<String, StoredStrategy> templates;

    public BundledStrategyTemplateCatalog(ObjectMapper objectMapper) {
        this.templates = DEFAULT_STRATEGIES.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> loadTemplate(objectMapper, entry.getKey(), entry.getValue())));
    }

    public List<StoredStrategy> findAll() {
        return templates.values().stream()
                .sorted(java.util.Comparator.comparing(StoredStrategy::name))
                .toList();
    }

    public Optional<StoredStrategy> findById(String id) {
        return Optional.ofNullable(templates.get(id));
    }

    private StoredStrategy loadTemplate(ObjectMapper objectMapper, String id, TemplateSeed templateSeed) {
        try (InputStream inputStream = getClass().getResourceAsStream(templateSeed.resourcePath())) {
            if (inputStream == null) {
                throw new IllegalStateException("Could not find strategy seed resource: " + templateSeed.resourcePath());
            }

            SeedStrategy seedStrategy = objectMapper.readValue(inputStream, new TypeReference<SeedStrategy>() {});
            return new StoredStrategy(
                    id,
                    templateSeed.name(),
                    StrategyKind.TEMPLATE,
                    null,
                    seedStrategy.nodes().stream()
                            .map(node -> new GraphNode(
                                    node.id(),
                                    node.type(),
                                    new NodePosition(node.position().x(), node.position().y()),
                                    normalizeNodeData(objectMapper, node.data())))
                            .toList(),
                    seedStrategy.edges().stream()
                            .map(edge -> new GraphEdge(
                                    edge.id(),
                                    edge.source(),
                                    edge.target(),
                                    edge.sourceHandle(),
                                    edge.targetHandle(),
                                    null,
                                    null))
                            .toList(),
                    Instant.EPOCH);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load strategy seed resource: " + templateSeed.resourcePath(), exception);
        }
    }

    private JsonNode normalizeNodeData(ObjectMapper objectMapper, JsonNode data) {
        return data != null && data.isObject() ? data.deepCopy() : objectMapper.createObjectNode();
    }

    private record TemplateSeed(String resourcePath, String name) {}

    private record SeedStrategy(List<SeedNode> nodes, List<SeedEdge> edges) {}

    private record SeedNode(String id, String type, SeedPosition position, JsonNode data) {}

    private record SeedPosition(double x, double y) {}

    private record SeedEdge(
            String id,
            String source,
            String target,
            String sourceHandle,
            String targetHandle) {}
}
