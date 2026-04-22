package dev.send.api.strategy.infra.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStrategyJpaRepository extends JpaRepository<UserStrategyEntity, String> {
    List<UserStrategyEntity> findAllByUserIdOrderByUpdatedAtDesc(String userId);

    Optional<UserStrategyEntity> findByStrategyIdAndUserId(String strategyId, String userId);
}
