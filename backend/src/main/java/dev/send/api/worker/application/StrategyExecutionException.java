package dev.send.api.worker.application;

import java.util.List;
import org.springframework.http.HttpStatus;

public class StrategyExecutionException extends RuntimeException {
  private final HttpStatus httpStatus;
  private final String code;
  private final List<String> details;

  public StrategyExecutionException(
      HttpStatus httpStatus, String code, String message, List<String> details) {
    super(message);
    this.httpStatus = httpStatus;
    this.code = code;
    this.details = List.copyOf(details);
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }

  public String code() {
    return code;
  }

  public List<String> details() {
    return details;
  }
}
