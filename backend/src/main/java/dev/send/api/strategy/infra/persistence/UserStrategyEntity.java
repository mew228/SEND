package dev.send.api.strategy.infra.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_strategies")
public class UserStrategyEntity {
    @Id
    @Column(name = "strategy_id", nullable = false, length = 100)
    private String strategyId = "";

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId = "";

    @Column(name = "name", nullable = false, length = 255)
    private String name = "";

    @Column(name = "graph_json", nullable = false, columnDefinition = "TEXT")
    private String graphJson = "";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    protected UserStrategyEntity() {}

    public UserStrategyEntity(
            String strategyId,
            String userId,
            String name,
            String graphJson,
            Instant createdAt,
            Instant updatedAt) {
        this.strategyId = Objects.requireNonNull(strategyId, "strategyId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.graphJson = Objects.requireNonNull(graphJson, "graphJson must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = Objects.requireNonNull(strategyId, "strategyId must not be null");
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public String getGraphJson() {
        return graphJson;
    }

    public void setGraphJson(String graphJson) {
        this.graphJson = Objects.requireNonNull(graphJson, "graphJson must not be null");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
