package dev.send.api.marketdata.domain;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nullable;

public record DailyStockPrice(
    String symbol,
    Instant time,
    @Nullable Double open,
    @Nullable Double high,
    @Nullable Double low,
    @Nullable Double close,
    @Nullable Long volume) {
  public DailyStockPrice {
    Objects.requireNonNull(symbol, "symbol must not be null");
    Objects.requireNonNull(time, "time must not be null");
  }
}
