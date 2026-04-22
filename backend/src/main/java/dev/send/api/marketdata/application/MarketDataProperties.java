package dev.send.api.marketdata.application;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public class MarketDataProperties {
  private List<String> bootstrapTrackedSymbols = List.of("AAPL", "SPY");
  private SchedulerProperties scheduler = new SchedulerProperties();

  public List<String> getBootstrapTrackedSymbols() {
    return bootstrapTrackedSymbols;
  }

  public void setBootstrapTrackedSymbols(List<String> bootstrapTrackedSymbols) {
    this.bootstrapTrackedSymbols =
        bootstrapTrackedSymbols == null ? List.of() : List.copyOf(bootstrapTrackedSymbols);
  }

  public SchedulerProperties getScheduler() {
    return scheduler;
  }

  public void setScheduler(SchedulerProperties scheduler) {
    this.scheduler = scheduler == null ? new SchedulerProperties() : scheduler;
  }

  public static class SchedulerProperties {
    private TaskProperties prices = new TaskProperties();
    private TaskProperties fundamentals = new TaskProperties();

    public TaskProperties getPrices() {
      return prices;
    }

    public void setPrices(TaskProperties prices) {
      this.prices = prices == null ? new TaskProperties() : prices;
    }

    public TaskProperties getFundamentals() {
      return fundamentals;
    }

    public void setFundamentals(TaskProperties fundamentals) {
      this.fundamentals = fundamentals == null ? new TaskProperties() : fundamentals;
    }
  }

  public static class TaskProperties {
    private boolean enabled = false;
    private String cron = "0 0 18 * * MON-FRI";
    private String zone = "America/New_York";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getCron() {
      return cron;
    }

    public void setCron(String cron) {
      this.cron = cron;
    }

    public String getZone() {
      return zone;
    }

    public void setZone(String zone) {
      this.zone = zone;
    }
  }
}
