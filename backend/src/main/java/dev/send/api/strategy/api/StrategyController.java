package dev.send.api.strategy.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.send.api.auth.CurrentUser;
import dev.send.api.auth.CurrentUserAccessor;
import dev.send.api.analytics.ClientAddressResolver;
import dev.send.api.catalog.api.dto.NodeIoCatalogDto;
import dev.send.api.catalog.application.NodeCatalogService;
import dev.send.api.strategy.api.dto.StoredStrategyDto;
import dev.send.api.strategy.api.dto.StrategyDocumentDto;
import dev.send.api.strategy.api.dto.StrategySimulationBoundsDto;
import dev.send.api.strategy.api.dto.StrategySimulationRequestDto;
import dev.send.api.strategy.api.dto.StrategySimulationResultDto;
import dev.send.api.strategy.api.dto.StrategySummaryDto;
import dev.send.api.strategy.api.dto.StrategyUpsertRequestDto;
import dev.send.api.strategy.application.StrategyDocumentMapper;
import dev.send.api.strategy.application.SimulationRateLimitException;
import dev.send.api.strategy.application.StrategySimulationRateLimiter;
import dev.send.api.strategy.application.StrategyService;
import dev.send.api.strategy.application.StrategySimulationBoundsService;
import dev.send.api.strategy.application.StrategyValidationException;
import dev.send.api.worker.application.StrategyExecutionException;
import dev.send.api.worker.application.StrategySimulationConfig;
import dev.send.api.worker.application.StrategyExecutionService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {
    private final StrategyService strategyService;
    private final StrategyDocumentMapper strategyDocumentMapper;
    private final NodeCatalogService nodeCatalogService;
    private final StrategyExecutionService strategyExecutionService;
    private final StrategySimulationBoundsService strategySimulationBoundsService;
    private final StrategySimulationRateLimiter strategySimulationRateLimiter;
    private final CurrentUserAccessor currentUserAccessor;
    private final ObjectMapper objectMapper;
    private final ClientAddressResolver clientAddressResolver;

    public StrategyController(
            StrategyService strategyService,
            StrategyDocumentMapper strategyDocumentMapper,
            NodeCatalogService nodeCatalogService,
            StrategyExecutionService strategyExecutionService,
            StrategySimulationBoundsService strategySimulationBoundsService,
            StrategySimulationRateLimiter strategySimulationRateLimiter,
            CurrentUserAccessor currentUserAccessor,
            ObjectMapper objectMapper,
            ClientAddressResolver clientAddressResolver) {
        this.strategyService = strategyService;
        this.strategyDocumentMapper = strategyDocumentMapper;
        this.nodeCatalogService = nodeCatalogService;
        this.strategyExecutionService = strategyExecutionService;
        this.strategySimulationBoundsService = strategySimulationBoundsService;
        this.strategySimulationRateLimiter = strategySimulationRateLimiter;
        this.currentUserAccessor = currentUserAccessor;
        this.objectMapper = objectMapper;
        this.clientAddressResolver = clientAddressResolver;
    }

    @GetMapping
    public Collection<StrategySummaryDto> getAll() {
        return strategyService.findAll(currentUserAccessor.findCurrentUser()).stream()
                .map(strategyDocumentMapper::toSummaryDto)
                .toList();
    }

    @PostMapping
    public StoredStrategyDto create(@RequestBody StrategyUpsertRequestDto strategyUpsertRequestDto) {
        CurrentUser currentUser = currentUserAccessor.requireCurrentUser();
        return strategyDocumentMapper.toStoredDto(
                strategyService.create(
                        currentUser,
                        strategyDocumentMapper.toCommand(null, strategyUpsertRequestDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoredStrategyDto> update(
            @PathVariable String id,
            @RequestBody StrategyUpsertRequestDto strategyUpsertRequestDto) {
        CurrentUser currentUser = currentUserAccessor.requireCurrentUser();
        return strategyService.update(
                currentUser,
                strategyDocumentMapper.toCommand(id, strategyUpsertRequestDto))
                .map(strategyDocumentMapper::toStoredDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/test")
    public JsonNode test(@RequestBody StrategyDocumentDto strategyDocumentDto) {
        return strategyExecutionService.executeGraphResults(strategyDocumentMapper.toDomain(strategyDocumentDto));
    }

    @PostMapping("/simulate")
    public StrategySimulationResultDto simulate(
            @RequestBody StrategySimulationRequestDto requestDto,
            HttpServletRequest httpServletRequest) {
        if (requestDto == null || requestDto.strategy() == null) {
            throw new StrategyValidationException("Simulation strategy is required.");
        }
        if (requestDto.simulation() == null) {
            throw new StrategyValidationException("Simulation config is required.");
        }
        strategySimulationRateLimiter.checkAllowed(resolveSimulationClientKey(httpServletRequest));

        StrategySimulationConfig simulationConfig = new StrategySimulationConfig(
                requestDto.simulation().startDate(),
                requestDto.simulation().endDate(),
                requestDto.simulation().initialCash(),
                requestDto.simulation().includeTrace() == null || requestDto.simulation().includeTrace());
        strategySimulationBoundsService.validateSimulationRequest(simulationConfig);
        JsonNode result = strategyExecutionService.simulateGraphResults(
                strategyDocumentMapper.toDomain(requestDto.strategy()),
                simulationConfig);
        return objectMapper.convertValue(result, StrategySimulationResultDto.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoredStrategyDto> getStrategy(@PathVariable String id) {
        Optional<CurrentUser> currentUser = currentUserAccessor.findCurrentUser();
        return strategyService.findById(id, currentUser)
                .map(strategyDocumentMapper::toStoredDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/node-io")
    public NodeIoCatalogDto getNodeIoCatalog() {
        return nodeCatalogService.getNodeIoCatalog();
    }

    @GetMapping("/simulation-bounds")
    public StrategySimulationBoundsDto getSimulationBounds() {
        var bounds = strategySimulationBoundsService.getSimulationBounds();
        return new StrategySimulationBoundsDto(
                bounds.hasPriceData(),
                bounds.earliestPriceDate(),
                bounds.latestPriceDate());
    }

    @ExceptionHandler(StrategyValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorDto handleValidationError(StrategyValidationException exception) {
        String message = exception.getMessage();
        return new ApiErrorDto(
                "strategy_validation_failed",
                message == null ? "Strategy validation failed." : message,
                List.of());
    }

    @ExceptionHandler(StrategyExecutionException.class)
    public ResponseEntity<ApiErrorDto> handleExecutionError(StrategyExecutionException exception) {
        String message = exception.getMessage();
        ApiErrorDto error = new ApiErrorDto(
                exception.code(),
                message == null ? "Strategy execution failed." : message,
                exception.details());
        return ResponseEntity.status(exception.httpStatus()).body(error);
    }

    @ExceptionHandler(SimulationRateLimitException.class)
    public ResponseEntity<ApiErrorDto> handleSimulationRateLimitError(SimulationRateLimitException exception) {
        long retryAfterMs = exception.getRetryAfterMs();
        long retryAfterSeconds = Math.max(1, (long) Math.ceil(retryAfterMs / 1000.0));
        String message = exception.getMessage();
        ApiErrorDto error = new ApiErrorDto(
                "simulation_rate_limited",
                message == null ? "Strategy simulations are limited to one run every 5 seconds." : message,
                List.of("retryAfterMs=" + retryAfterMs));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(error);
    }

    private String resolveSimulationClientKey(HttpServletRequest httpServletRequest) {
        return clientAddressResolver.resolve(httpServletRequest);
    }
}
