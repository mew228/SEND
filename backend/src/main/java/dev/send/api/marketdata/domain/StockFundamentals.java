package dev.send.api.marketdata.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public record StockFundamentals(
    String symbol,
    @Nullable Double eps,
    @Nullable Double peRatio,
    @Nullable Double beta,
    @Nullable LocalDate asOfDate,
    Map<String, Double> extraMetrics,
    Instant refreshedAt) {
  public StockFundamentals {
    Objects.requireNonNull(symbol, "symbol must not be null");
    extraMetrics = Map.copyOf(extraMetrics);
    Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
  }
}
