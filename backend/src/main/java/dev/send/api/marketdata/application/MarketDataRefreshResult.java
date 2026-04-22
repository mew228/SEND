package dev.send.api.marketdata.application;

import java.util.List;
import java.util.Objects;

public record MarketDataRefreshResult(
    String dataset, String scope, List<String> requestedSymbols, int recordsWritten) {
  public MarketDataRefreshResult {
    Objects.requireNonNull(dataset, "dataset must not be null");
    Objects.requireNonNull(scope, "scope must not be null");
    requestedSymbols = List.copyOf(requestedSymbols);
  }
}
