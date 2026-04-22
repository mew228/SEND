package dev.send.api.strategy.application;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StrategySimulationRateLimiter {
  private final ConcurrentMap<String, Instant> nextAllowedRequestByClient =
      new ConcurrentHashMap<>();
  private final StrategyRateLimitProperties properties;
  private final Clock clock;

  @Autowired
  public StrategySimulationRateLimiter(StrategyRateLimitProperties properties) {
    this(properties, Clock.systemUTC());
  }

  StrategySimulationRateLimiter(StrategyRateLimitProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public void checkAllowed(String clientKey) {
    long cooldownMs = properties.getSimulationCooldownMs();
    if (cooldownMs <= 0) {
      return;
    }

    Instant now = clock.instant();
    AtomicLong retryAfterMs = new AtomicLong(0);
    Instant nextAllowedAt =
        nextAllowedRequestByClient.compute(
            clientKey,
            (ignored, currentNextAllowedAt) -> {
              if (currentNextAllowedAt != null && currentNextAllowedAt.isAfter(now)) {
                retryAfterMs.set(
                    Math.max(0, currentNextAllowedAt.toEpochMilli() - now.toEpochMilli()));
                return currentNextAllowedAt;
              }
              return now.plusMillis(cooldownMs);
            });

    if (nextAllowedAt != null && retryAfterMs.get() > 0) {
      throw new SimulationRateLimitException(buildCooldownMessage(cooldownMs), retryAfterMs.get());
    }
  }

  private String buildCooldownMessage(long cooldownMs) {
    long cooldownSeconds = Math.max(1, (long) Math.ceil(cooldownMs / 1000.0));
    return "Strategy simulations are limited to one run every " + cooldownSeconds + " seconds.";
  }
}
