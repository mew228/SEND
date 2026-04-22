package dev.send.api.marketdata.application;

import dev.send.api.marketdata.domain.StockFundamentals;
import java.util.Optional;

public interface FundamentalsDataProvider {
  Optional<StockFundamentals> fetchFundamentals(String symbol);
}
