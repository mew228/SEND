package dev.send.api.strategy.domain;

import java.util.List;
import java.util.Optional;

public interface StrategyRepository {
  List<StrategyDocument> findAll();

  Optional<StrategyDocument> findById(String id);

  StrategyDocument save(StrategyDocument strategyDocument);
}
