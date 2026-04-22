package dev.send.api.scheduling;

import dev.send.api.marketdata.application.MarketDataProperties;
import dev.send.api.marketdata.application.MarketDataRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {
  private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

  private final MarketDataRefreshService marketDataRefreshService;
  private final MarketDataProperties marketDataProperties;

  public SchedulingConfig(
      MarketDataRefreshService marketDataRefreshService,
      MarketDataProperties marketDataProperties) {
    this.marketDataRefreshService = marketDataRefreshService;
    this.marketDataProperties = marketDataProperties;
  }

  @Scheduled(
      cron = "${market-data.scheduler.prices.cron}",
      zone = "${market-data.scheduler.prices.zone}")
  public void refreshTrackedPrices() {
    if (!marketDataProperties.getScheduler().getPrices().isEnabled()) {
      return;
    }
    try {
      marketDataRefreshService.refreshTrackedPrices();
    } catch (RuntimeException exception) {
      log.warn("Scheduled price refresh failed: {}", exception.getMessage());
    }
  }

  @Scheduled(
      cron = "${market-data.scheduler.fundamentals.cron}",
      zone = "${market-data.scheduler.fundamentals.zone}")
  public void refreshTrackedFundamentals() {
    if (!marketDataProperties.getScheduler().getFundamentals().isEnabled()) {
      return;
    }
    try {
      marketDataRefreshService.refreshTrackedFundamentals();
    } catch (RuntimeException exception) {
      log.warn("Scheduled fundamentals refresh failed: {}", exception.getMessage());
    }
  }
}
