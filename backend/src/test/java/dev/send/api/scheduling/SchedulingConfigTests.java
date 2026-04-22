package dev.send.api.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.send.api.marketdata.application.MarketDataProperties;
import dev.send.api.marketdata.application.MarketDataRefreshService;
import org.junit.jupiter.api.Test;

class SchedulingConfigTests {
  private final MarketDataRefreshService marketDataRefreshService =
      mock(MarketDataRefreshService.class);

  @Test
  void skipsPriceRefreshWhenDisabled() {
    MarketDataProperties properties = new MarketDataProperties();
    properties.getScheduler().getPrices().setEnabled(false);

    SchedulingConfig schedulingConfig = new SchedulingConfig(marketDataRefreshService, properties);

    schedulingConfig.refreshTrackedPrices();

    verifyNoInteractions(marketDataRefreshService);
  }

  @Test
  void runsFundamentalsRefreshWhenEnabled() {
    MarketDataProperties properties = new MarketDataProperties();
    properties.getScheduler().getFundamentals().setEnabled(true);

    SchedulingConfig schedulingConfig = new SchedulingConfig(marketDataRefreshService, properties);

    schedulingConfig.refreshTrackedFundamentals();

    verify(marketDataRefreshService).refreshTrackedFundamentals();
  }
}
