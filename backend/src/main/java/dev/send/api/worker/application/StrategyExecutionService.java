package dev.send.api.worker.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.send.api.strategy.application.StrategyGraphValidator;
import dev.send.api.strategy.application.StrategyValidationException;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.worker.infra.ocaml.OcamlExecutionRequest;
import dev.send.api.worker.infra.ocaml.OcamlExecutionResponse;
import dev.send.api.worker.infra.ocaml.OcamlWorkerClient;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Bridge-oriented execution service. Java keeps transport validation and graph storage, while OCaml
 * owns semantic validation and execution.
 */
@Service
public class StrategyExecutionService {
  public static final double MAX_INITIAL_CASH = 1_000_000_000d;

  private final StrategyGraphValidator strategyGraphValidator;
  private final OcamlWorkerClient ocamlWorkerClient;

  public StrategyExecutionService(
      StrategyGraphValidator strategyGraphValidator, OcamlWorkerClient ocamlWorkerClient) {
    this.strategyGraphValidator = strategyGraphValidator;
    this.ocamlWorkerClient = ocamlWorkerClient;
  }

  public OcamlExecutionRequest createValidateRequest(StrategyDocument strategyDocument) {
    strategyGraphValidator.validate(strategyDocument);
    return ocamlWorkerClient.createValidateRequest(strategyDocument);
  }

  public OcamlExecutionRequest createExecuteRequest(StrategyDocument strategyDocument) {
    strategyGraphValidator.validate(strategyDocument);
    return ocamlWorkerClient.createExecuteRequest(strategyDocument);
  }

  public OcamlExecutionRequest createSimulateRequest(
      StrategyDocument strategyDocument, StrategySimulationConfig simulationConfig) {
    strategyGraphValidator.validate(strategyDocument);
    validateSimulationConfig(simulationConfig);
    return ocamlWorkerClient.createSimulateRequest(strategyDocument, simulationConfig);
  }

  public OcamlExecutionResponse validateGraph(StrategyDocument strategyDocument) {
    strategyGraphValidator.validate(strategyDocument);
    return ocamlWorkerClient.validateGraph(strategyDocument);
  }

  public OcamlExecutionResponse executeGraph(StrategyDocument strategyDocument) {
    strategyGraphValidator.validate(strategyDocument);
    try {
      OcamlExecutionResponse response = ocamlWorkerClient.executeGraph(strategyDocument);
      if (!"ok".equals(response.status())) {
        throw mapFailure(response);
      }

      JsonNode result = response.result();
      if (!(result instanceof ObjectNode resultObject)) {
        throw workerFailure(
            "protocol_error", "OCaml worker returned a malformed execution result.", List.of());
      }
      return new OcamlExecutionResponse(
          response.status(),
          response.command(),
          resultObject.deepCopy(),
          response.code(),
          response.message(),
          response.details());
    } catch (StrategyExecutionException exception) {
      throw exception;
    } catch (IllegalStateException exception) {
      throw workerFailure(
          "worker_process_failed",
          "Java could not complete the OCaml worker request.",
          exception.getMessage() == null ? List.of() : List.of(exception.getMessage()));
    }
  }

  public OcamlExecutionResponse simulateGraph(
      StrategyDocument strategyDocument, StrategySimulationConfig simulationConfig) {
    strategyGraphValidator.validate(strategyDocument);
    validateSimulationConfig(simulationConfig);
    try {
      OcamlExecutionResponse response =
          ocamlWorkerClient.simulateGraph(strategyDocument, simulationConfig);
      if (!"ok".equals(response.status())) {
        throw mapFailure(response);
      }

      JsonNode result = response.result();
      if (!(result instanceof ObjectNode resultObject)) {
        throw workerFailure(
            "protocol_error", "OCaml worker returned a malformed simulation result.", List.of());
      }
      return new OcamlExecutionResponse(
          response.status(),
          response.command(),
          resultObject.deepCopy(),
          response.code(),
          response.message(),
          response.details());
    } catch (StrategyExecutionException exception) {
      throw exception;
    } catch (IllegalStateException exception) {
      throw workerFailure(
          "worker_process_failed",
          "Java could not complete the OCaml worker request.",
          exception.getMessage() == null ? List.of() : List.of(exception.getMessage()));
    }
  }

  public ObjectNode executeGraphResults(StrategyDocument strategyDocument) {
    OcamlExecutionResponse response = executeGraph(strategyDocument);
    JsonNode result = response.result();
    if (result instanceof ObjectNode objectNode) {
      return objectNode.deepCopy();
    }
    throw workerFailure(
        "protocol_error", "OCaml worker returned a malformed execution result.", List.of());
  }

  public ObjectNode simulateGraphResults(
      StrategyDocument strategyDocument, StrategySimulationConfig simulationConfig) {
    OcamlExecutionResponse response = simulateGraph(strategyDocument, simulationConfig);
    JsonNode result = response.result();
    if (result instanceof ObjectNode objectNode) {
      return objectNode.deepCopy();
    }
    throw workerFailure(
        "protocol_error", "OCaml worker returned a malformed simulation result.", List.of());
  }

  private StrategyExecutionException mapFailure(OcamlExecutionResponse response) {
    String code =
        response.code() == null || response.code().isBlank() ? "worker_failure" : response.code();
    String message =
        response.message() == null || response.message().isBlank()
            ? "OCaml worker execution failed."
            : response.message();

    HttpStatus status =
        switch (code) {
          case "spec_decode_failed",
                  "graph_validation_failed",
                  "execution_failed",
                  "missing_executor" ->
              HttpStatus.UNPROCESSABLE_ENTITY;
          default -> HttpStatus.BAD_GATEWAY;
        };
    return new StrategyExecutionException(status, code, message, response.details());
  }

  private StrategyExecutionException workerFailure(
      String code, String message, List<String> details) {
    return new StrategyExecutionException(HttpStatus.BAD_GATEWAY, code, message, details);
  }

  private void validateSimulationConfig(StrategySimulationConfig simulationConfig) {
    if (simulationConfig.startDate() == null || simulationConfig.startDate().isBlank()) {
      throw new StrategyValidationException("Simulation startDate is required.");
    }
    if (simulationConfig.endDate() == null || simulationConfig.endDate().isBlank()) {
      throw new StrategyValidationException("Simulation endDate is required.");
    }
    if (!Double.isFinite(simulationConfig.initialCash())) {
      throw new StrategyValidationException("Simulation initialCash must be a finite number.");
    }
    if (simulationConfig.initialCash() < 0) {
      throw new StrategyValidationException("Simulation initialCash must be non-negative.");
    }
    if (simulationConfig.initialCash() > MAX_INITIAL_CASH) {
      throw new StrategyValidationException(
          "Simulation initialCash must be less than or equal to " + (long) MAX_INITIAL_CASH + ".");
    }
  }
}
