package dev.send.api.strategy.application;

public class SimulationRateLimitException extends RuntimeException {
  private final long retryAfterMs;

  public SimulationRateLimitException(String message, long retryAfterMs) {
    super(message);
    this.retryAfterMs = retryAfterMs;
  }

  public long getRetryAfterMs() {
    return retryAfterMs;
  }
}
