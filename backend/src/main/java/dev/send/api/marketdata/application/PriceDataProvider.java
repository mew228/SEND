package dev.send.api.marketdata.application;

import dev.send.api.marketdata.domain.DailyStockPrice;
import java.time.Instant;
import java.util.List;

public interface PriceDataProvider {
  List<DailyStockPrice> fetchHourlyPrices(String symbol, Instant startTime, int hourCount);
}
