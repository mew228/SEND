package dev.send.api.worker.infra.ocaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.send.api.strategy.domain.StrategyDocument;
import dev.send.api.worker.application.StrategySimulationConfig;
import dev.send.api.worker.application.StrategyWorkerPayloadMapper;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JSON-lines bridge to the OCaml worker process.
 *
 * <p>The worker command is optional for now. When not configured, callers can still use the
 * request-building methods to keep the boundary stable while the OCaml validate/execute handlers
 * are brought online.
 */
@Component
public class OcamlWorkerClient {
  private static final Logger log = LoggerFactory.getLogger(OcamlWorkerClient.class);

  private final ObjectMapper objectMapper;
  private final StrategyWorkerPayloadMapper strategyWorkerPayloadMapper;
  private final String workerCommand;

  @Nullable private Process process;
  @Nullable private BufferedWriter writer;
  @Nullable private BufferedReader reader;

  public OcamlWorkerClient(
      ObjectMapper objectMapper,
      StrategyWorkerPayloadMapper strategyWorkerPayloadMapper,
      @Value("${ocaml.worker.command:}") String workerCommand) {
    this.objectMapper = objectMapper;
    this.strategyWorkerPayloadMapper = strategyWorkerPayloadMapper;
    this.workerCommand = workerCommand;
  }

  public OcamlExecutionRequest createValidateRequest(StrategyDocument strategyDocument) {
    return new OcamlExecutionRequest(
        "validate_graph", strategyWorkerPayloadMapper.toWorkerPayload(strategyDocument));
  }

  public OcamlExecutionRequest createExecuteRequest(StrategyDocument strategyDocument) {
    return new OcamlExecutionRequest(
        "execute_graph", strategyWorkerPayloadMapper.toWorkerPayload(strategyDocument));
  }

  public OcamlExecutionRequest createSimulateRequest(
      StrategyDocument strategyDocument, StrategySimulationConfig simulationConfig) {
    return new OcamlExecutionRequest(
        "simulate_graph",
        strategyWorkerPayloadMapper.toSimulationPayload(strategyDocument, simulationConfig));
  }

  public OcamlExecutionResponse validateGraph(StrategyDocument strategyDocument) {
    return send(createValidateRequest(strategyDocument));
  }

  public OcamlExecutionResponse executeGraph(StrategyDocument strategyDocument) {
    return send(createExecuteRequest(strategyDocument));
  }

  public OcamlExecutionResponse simulateGraph(
      StrategyDocument strategyDocument, StrategySimulationConfig simulationConfig) {
    return send(createSimulateRequest(strategyDocument, simulationConfig));
  }

  public String encodeRequest(OcamlExecutionRequest request) {
    ObjectNode json = objectMapper.createObjectNode();
    json.put("command", request.command());
    if (request.payload() != null) {
      json.set("payload", request.payload());
    }
    return json.toString();
  }

  public OcamlExecutionResponse decodeResponse(String line) {
    try {
      JsonNode json = objectMapper.readTree(line);
      return new OcamlExecutionResponse(
          json.path("status").asText(""),
          textOrNull(json.get("command")),
          json.get("result"),
          textOrNull(json.get("code")),
          textOrNull(json.get("message")),
          toStringList(json.get("details")));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to decode OCaml worker response.", e);
    }
  }

  private synchronized OcamlExecutionResponse send(OcamlExecutionRequest request) {
    ensureProcess();
    if (writer == null || reader == null) {
      throw new IllegalStateException("OCaml worker streams are not initialized.");
    }

    try {
      writer.write(encodeRequest(request));
      writer.newLine();
      writer.flush();

      String line = reader.readLine();
      if (line == null) {
        throw new IllegalStateException("OCaml worker closed stdout unexpectedly.");
      }
      return decodeResponse(line);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to exchange data with the OCaml worker.", e);
    }
  }

  private void ensureProcess() {
    if (process != null && process.isAlive()) {
      return;
    }
    String resolvedWorkerCommand = resolveWorkerCommand();
    if (resolvedWorkerCommand == null || resolvedWorkerCommand.isBlank()) {
      throw new IllegalStateException(
          "OCaml worker command is not configured. Set ocaml.worker.command to enable worker calls.");
    }

    try {
      process = startWorkerProcess(resolvedWorkerCommand);
      writer =
          new BufferedWriter(
              new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      startStderrDrainer(process);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to start the OCaml worker process.", e);
    }
  }

  private String resolveWorkerCommand() {
    if (workerCommand != null && !workerCommand.isBlank()) {
      return workerCommand;
    }

    Path currentDirectory = Path.of("").toAbsolutePath();
    List<Path> candidates =
        List.of(
            currentDirectory.resolve("ocaml-engine"),
            currentDirectory.resolve("../ocaml-engine").normalize());

    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("dune-project"))) {
        Path builtWorker = candidate.resolve("_build/default/bin/worker.exe");
        if (isWindows() && Files.isRegularFile(builtWorker)) {
          return builtWorker.toString();
        }
        return "cd " + shellEscape(candidate.toString()) + " && dune exec ./bin/worker.exe";
      }
    }

    return workerCommand;
  }

  @Nullable
  private String textOrNull(@Nullable JsonNode jsonNode) {
    if (jsonNode == null || jsonNode.isNull()) {
      return null;
    }
    return jsonNode.asText();
  }

  private List<String> toStringList(@Nullable JsonNode jsonNode) {
    if (jsonNode == null || !jsonNode.isArray()) {
      return List.of();
    }

    List<String> values = new ArrayList<>();
    for (JsonNode value : jsonNode) {
      if (value.isTextual()) {
        values.add(value.asText());
      }
    }
    return List.copyOf(values);
  }

  private String shellEscape(String value) {
    return "'" + value.replace("'", "'\"'\"'") + "'";
  }

  private Process startWorkerProcess(String resolvedWorkerCommand) throws IOException {
    if (isWindowsExecutablePath(resolvedWorkerCommand)) {
      log.info("Starting OCaml worker executable at {}", resolvedWorkerCommand);
      return new ProcessBuilder(resolvedWorkerCommand).start();
    }

    if (isWindows()) {
      log.info("Starting OCaml worker via cmd: {}", resolvedWorkerCommand);
      return new ProcessBuilder("cmd.exe", "/c", resolvedWorkerCommand).start();
    }

    log.info("Starting OCaml worker via sh -lc");
    return new ProcessBuilder("sh", "-lc", resolvedWorkerCommand).start();
  }

  private Thread startStderrDrainer(Process workerProcess) {
    Thread thread =
        new Thread(
            () -> {
              try (BufferedReader stderr =
                  new BufferedReader(
                      new InputStreamReader(
                          workerProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = stderr.readLine()) != null) {
                  log.warn("OCaml worker stderr: {}", line);
                }
              } catch (IOException exception) {
                log.warn("Failed to read OCaml worker stderr.", exception);
              }
            },
            "ocaml-worker-stderr");
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  private boolean isWindowsExecutablePath(String command) {
    return isWindows()
        && command.toLowerCase(Locale.ROOT).endsWith(".exe")
        && Files.isRegularFile(Path.of(command));
  }

  private boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }

  @PreDestroy
  void close() {
    if (process != null) {
      process.destroy();
    }
  }
}
