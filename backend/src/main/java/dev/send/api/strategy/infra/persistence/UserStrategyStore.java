package dev.send.api.strategy.infra.persistence;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.send.api.strategy.domain.GraphEdge;
import dev.send.api.strategy.domain.GraphNode;
import dev.send.api.strategy.domain.StoredStrategy;
import dev.send.api.strategy.domain.StrategyKind;

@Component
public class UserStrategyStore {
    private final UserStrategyJpaRepository repository;
    private final ObjectMapper objectMapper;

    public UserStrategyStore(UserStrategyJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<StoredStrategy> findAllByUserId(String userId) {
        return repository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<StoredStrategy> findByStrategyIdAndUserId(String strategyId, String userId) {
        return repository.findByStrategyIdAndUserId(strategyId, userId).map(this::toDomain);
    }

    public StoredStrategy save(StoredStrategy strategy) {
        Instant now = Instant.now();
        UserStrategyEntity existing = repository.findById(strategy.id()).orElse(null);
        Instant createdAt = existing == null ? now : existing.getCreatedAt();
        UserStrategyEntity entity = new UserStrategyEntity(
                strategy.id(),
                requireOwner(strategy),
                strategy.name(),
                serializeGraph(strategy.nodes(), strategy.edges()),
                createdAt,
                now);
        repository.save(entity);
        return toDomain(entity);
    }

    private StoredStrategy toDomain(UserStrategyEntity entity) {
        StoredGraphPayload payload = deserializeGraph(entity.getGraphJson());
        return new StoredStrategy(
                entity.getStrategyId(),
                entity.getName(),
                StrategyKind.USER,
                entity.getUserId(),
                payload.nodes(),
                payload.edges(),
                entity.getUpdatedAt());
    }

    private String requireOwner(StoredStrategy strategy) {
        if (strategy.ownerUserId() == null || strategy.ownerUserId().isBlank()) {
            throw new IllegalArgumentException("User strategies require an owner user id.");
        }
        return strategy.ownerUserId();
    }

    private String serializeGraph(List<GraphNode> nodes, List<GraphEdge> edges) {
        try {
            return objectMapper.writeValueAsString(new StoredGraphPayload(nodes, edges));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize strategy graph.", exception);
        }
    }

    private StoredGraphPayload deserializeGraph(String graphJson) {
        try {
            return objectMapper.readValue(graphJson, StoredGraphPayload.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not deserialize strategy graph.", exception);
        }
    }

    private record StoredGraphPayload(List<GraphNode> nodes, List<GraphEdge> edges) {}
}
