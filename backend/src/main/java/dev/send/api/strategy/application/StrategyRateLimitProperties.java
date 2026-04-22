package dev.send.api.strategy.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strategy.rate-limit")
public class StrategyRateLimitProperties {
  private long simulationCooldownMs = 5000;

  public long getSimulationCooldownMs() {
    return simulationCooldownMs;
  }

  public void setSimulationCooldownMs(long simulationCooldownMs) {
    this.simulationCooldownMs = Math.max(simulationCooldownMs, 0);
  }
}
