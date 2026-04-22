package dev.send.api.marketdata.application;

import dev.send.api.marketdata.domain.DailyStockPrice;
import dev.send.api.marketdata.domain.StockFundamentals;
import dev.send.api.marketdata.infra.persistence.StockFundamentalsEntity;
import dev.send.api.marketdata.infra.persistence.StockFundamentalsRepository;
import dev.send.api.marketdata.infra.persistence.StockPriceJdbcRepository;
import dev.send.api.marketdata.infra.persistence.TrackedTickerJdbcRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketDataRefreshService {
  private final MarketDataProperties marketDataProperties;
  private final StockPriceJdbcRepository stockPriceJdbcRepository;
  private final StockFundamentalsRepository stockFundamentalsRepository;
  private final TrackedTickerJdbcRepository trackedTickerJdbcRepository;
  private final Optional<PriceDataProvider> priceDataProvider;
  private final Optional<FundamentalsDataProvider> fundamentalsDataProvider;

  public MarketDataRefreshService(
      MarketDataProperties marketDataProperties,
      StockPriceJdbcRepository stockPriceJdbcRepository,
      StockFundamentalsRepository stockFundamentalsRepository,
      TrackedTickerJdbcRepository trackedTickerJdbcRepository,
      Optional<PriceDataProvider> priceDataProvider,
      Optional<FundamentalsDataProvider> fundamentalsDataProvider) {
    this.marketDataProperties = marketDataProperties;
    this.stockPriceJdbcRepository = stockPriceJdbcRepository;
    this.stockFundamentalsRepository = stockFundamentalsRepository;
    this.trackedTickerJdbcRepository = trackedTickerJdbcRepository;
    this.priceDataProvider = priceDataProvider;
    this.fundamentalsDataProvider = fundamentalsDataProvider;
  }

  @PostConstruct
  public void seedTrackedSymbols() {
    for (String symbol : normalizeSymbols(marketDataProperties.getBootstrapTrackedSymbols())) {
      trackedTickerJdbcRepository.insertIfAbsent(symbol);
    }
  }

  public MarketDataRefreshResult refreshTrackedPrices() {
    return refreshPrices(trackedTickerJdbcRepository.findEnabledSymbols(), "tracked", 1);
  }

  public MarketDataRefreshResult refreshTrackedFundamentals() {
    return refreshFundamentals(trackedTickerJdbcRepository.findEnabledSymbols(), "tracked");
  }

  public MarketDataRefreshResult refreshPrice(String symbol) {
    return refreshPrices(List.of(normalizeSymbol(symbol)), "single", 1);
  }

  public MarketDataRefreshResult refreshPrice(String symbol, int length) {
    return refreshPrices(List.of(normalizeSymbol(symbol)), "single", length);
  }

  public MarketDataRefreshResult refreshTrackedPrices(int length) {
    return refreshPrices(trackedTickerJdbcRepository.findEnabledSymbols(), "tracked", length);
  }

  public MarketDataRefreshResult refreshFundamentals(String symbol) {
    return refreshFundamentals(List.of(normalizeSymbol(symbol)), "single");
  }

  public List<TrackedTickerJdbcRepository.TrackedTicker> listTrackedTickers() {
    return trackedTickerJdbcRepository.findAll();
  }

  public List<TrackedTickerJdbcRepository.TrackedTicker> addTrackedTicker(String symbol) {
    trackedTickerJdbcRepository.upsert(normalizeSymbol(symbol), true);
    return trackedTickerJdbcRepository.findAll();
  }

  public List<TrackedTickerJdbcRepository.TrackedTicker> setTrackedTickerEnabled(
      String symbol, boolean enabled) {
    trackedTickerJdbcRepository.updateEnabled(normalizeSymbol(symbol), enabled);
    return trackedTickerJdbcRepository.findAll();
  }

  public List<TrackedTickerJdbcRepository.TrackedTicker> deleteTrackedTicker(String symbol) {
    trackedTickerJdbcRepository.delete(normalizeSymbol(symbol));
    return trackedTickerJdbcRepository.findAll();
  }

  private MarketDataRefreshResult refreshPrices(List<String> symbols, String scope, int length) {
    int normalizedLength = normalizeLength(length);
    Duration stepDuration = Duration.ofHours(1);
    PriceDataProvider provider =
        priceDataProvider.orElseThrow(
            () -> new IllegalStateException("No price data provider is configured."));
    int recordsWritten = 0;
    for (String symbol : symbols) {
      Instant requestedEndTime = alignToHour(Instant.now());
      Instant requestedStartTime =
          requestedEndTime.minus(stepDuration.multipliedBy(normalizedLength - 1L));
      Instant fetchStartTime = resolveFetchStartTime(symbol, requestedStartTime, stepDuration);
      if (fetchStartTime.isAfter(requestedEndTime)) {
        continue;
      }

      int hourCount =
          Math.toIntExact(Duration.between(fetchStartTime, requestedEndTime).toHours()) + 1;
      List<DailyStockPrice> stockPrices =
          provider.fetchHourlyPrices(symbol, fetchStartTime, hourCount);
      ensureCompleteHourlySeries(symbol, fetchStartTime, hourCount, stepDuration, stockPrices);
      for (DailyStockPrice stockPrice : stockPrices) {
        recordsWritten += stockPriceJdbcRepository.upsert(stockPrice);
      }
    }
    return new MarketDataRefreshResult("prices", scope, symbols, recordsWritten);
  }

  private MarketDataRefreshResult refreshFundamentals(List<String> symbols, String scope) {
    FundamentalsDataProvider provider =
        fundamentalsDataProvider.orElseThrow(
            () -> new IllegalStateException("No fundamentals data provider is configured."));
    int recordsWritten = 0;
    for (String symbol : symbols) {
      Optional<StockFundamentals> fundamentals = provider.fetchFundamentals(symbol);
      if (fundamentals.isPresent()) {
        stockFundamentalsRepository.save(toEntity(fundamentals.get()));
        recordsWritten += 1;
      }
    }
    return new MarketDataRefreshResult("fundamentals", scope, symbols, recordsWritten);
  }

  private StockFundamentalsEntity toEntity(StockFundamentals fundamentals) {
    return new StockFundamentalsEntity(
        fundamentals.symbol(),
        fundamentals.eps(),
        fundamentals.peRatio(),
        fundamentals.beta(),
        fundamentals.asOfDate(),
        sanitizeMetrics(fundamentals.extraMetrics()),
        fundamentals.refreshedAt());
  }

  private Map<String, Double> sanitizeMetrics(Map<String, Double> rawMetrics) {
    Objects.requireNonNull(rawMetrics, "rawMetrics must not be null");
    return Map.copyOf(rawMetrics);
  }

  private Instant resolveFetchStartTime(
      String symbol, Instant requestedStartTime, Duration stepDuration) {
    Instant alignedRequestedStartTime = alignToHour(requestedStartTime);
    return stockPriceJdbcRepository
        .findLatestTime(symbol)
        .map(this::alignToHour)
        .map(
            lastTime -> {
              Instant nextMissingTime = lastTime.plus(stepDuration);
              return nextMissingTime.isAfter(alignedRequestedStartTime)
                  ? nextMissingTime
                  : alignedRequestedStartTime;
            })
        .orElse(alignedRequestedStartTime);
  }

  private void ensureCompleteHourlySeries(
      String symbol,
      Instant startTime,
      int hourCount,
      Duration stepDuration,
      List<DailyStockPrice> stockPrices) {
    if (stockPrices.size() != hourCount) {
      throw new IllegalStateException(
          "Price provider returned incomplete hourly data for " + symbol + ".");
    }

    for (int index = 0; index < stockPrices.size(); index++) {
      DailyStockPrice stockPrice = stockPrices.get(index);
      Instant expectedTime = startTime.plus(stepDuration.multipliedBy(index));
      if (!stockPrice.symbol().equals(symbol)
          || !alignToHour(stockPrice.time()).equals(expectedTime)) {
        throw new IllegalStateException(
            "Price provider returned non-contiguous hourly data for " + symbol + ".");
      }
    }
  }

  private int normalizeLength(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Length must be greater than zero.");
    }
    return length;
  }

  private Instant alignToHour(Instant instant) {
    return instant.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
  }

  private List<String> normalizeSymbols(List<String> rawSymbols) {
    Objects.requireNonNull(rawSymbols, "rawSymbols must not be null");
    LinkedHashSet<String> normalizedSymbols = new LinkedHashSet<>();
    for (String rawSymbol : rawSymbols) {
      String normalized = normalizeSymbol(rawSymbol);
      normalizedSymbols.add(normalized);
    }
    return List.copyOf(new ArrayList<>(normalizedSymbols));
  }

  private String normalizeSymbol(String rawSymbol) {
    if (rawSymbol == null) {
      throw new IllegalArgumentException("Symbol must not be null.");
    }
    String normalized = rawSymbol.trim().toUpperCase(Locale.US);
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Symbol must not be blank.");
    }
    return normalized;
  }
}
