package dev.send.api.marketdata.api;

import dev.send.api.marketdata.application.MarketDataRefreshResult;
import dev.send.api.marketdata.application.MarketDataRefreshService;
import dev.send.api.marketdata.infra.persistence.TrackedTickerJdbcRepository;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/market-data")
public class MarketDataAdminController {
  private final MarketDataRefreshService marketDataRefreshService;

  public MarketDataAdminController(MarketDataRefreshService marketDataRefreshService) {
    this.marketDataRefreshService = marketDataRefreshService;
  }

  @GetMapping("/tickers")
  public List<TrackedTickerJdbcRepository.TrackedTicker> listTickers() {
    return marketDataRefreshService.listTrackedTickers();
  }

  @PostMapping("/tickers")
  public List<TrackedTickerJdbcRepository.TrackedTicker> addTicker(
      @RequestParam("symbol") String symbol) {
    return executeBadRequest(() -> marketDataRefreshService.addTrackedTicker(symbol));
  }

  @PostMapping("/tickers/{symbol}/enable")
  public List<TrackedTickerJdbcRepository.TrackedTicker> enableTicker(@PathVariable String symbol) {
    return executeBadRequest(() -> marketDataRefreshService.setTrackedTickerEnabled(symbol, true));
  }

  @PostMapping("/tickers/{symbol}/disable")
  public List<TrackedTickerJdbcRepository.TrackedTicker> disableTicker(
      @PathVariable String symbol) {
    return executeBadRequest(() -> marketDataRefreshService.setTrackedTickerEnabled(symbol, false));
  }

  @DeleteMapping("/tickers/{symbol}")
  public List<TrackedTickerJdbcRepository.TrackedTicker> deleteTicker(@PathVariable String symbol) {
    return executeBadRequest(() -> marketDataRefreshService.deleteTrackedTicker(symbol));
  }

  @PostMapping("/prices/refresh")
  public MarketDataRefreshResult refreshPrices(
      @RequestParam(name = "symbol", required = false) @Nullable String symbol,
      @RequestParam(name = "length", defaultValue = "1") int length) {
    return executeRefresh(
        () -> {
          if (symbol == null) {
            return marketDataRefreshService.refreshTrackedPrices(length);
          }
          return marketDataRefreshService.refreshPrice(symbol, length);
        });
  }

  @PostMapping("/fundamentals/refresh")
  public MarketDataRefreshResult refreshFundamentals(
      @RequestParam(name = "symbol", required = false) @Nullable String symbol) {
    return executeRefresh(
        () -> {
          if (symbol == null) {
            return marketDataRefreshService.refreshTrackedFundamentals();
          }
          return marketDataRefreshService.refreshFundamentals(symbol);
        });
  }

  private MarketDataRefreshResult executeRefresh(MarketDataCall<MarketDataRefreshResult> call) {
    try {
      return call.get();
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    } catch (IllegalStateException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
    }
  }

  private List<TrackedTickerJdbcRepository.TrackedTicker> executeBadRequest(
      MarketDataCall<List<TrackedTickerJdbcRepository.TrackedTicker>> call) {
    try {
      return call.get();
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }
  }

  @FunctionalInterface
  private interface MarketDataCall<T> {
    T get();
  }
}
