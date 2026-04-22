package dev.send.api.strategy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.send.api.marketdata.infra.persistence.StockPriceJdbcRepository;
import dev.send.api.strategy.application.StrategySimulationBoundsService;
import dev.send.api.strategy.application.StrategyValidationException;
import dev.send.api.worker.application.StrategySimulationConfig;

class StrategySimulationBoundsServiceTests {
    private final StockPriceJdbcRepository stockPriceJdbcRepository = mock(StockPriceJdbcRepository.class);
    private final StrategySimulationBoundsService strategySimulationBoundsService =
            new StrategySimulationBoundsService(stockPriceJdbcRepository);

    @Test
    void rejectsSimulationRangesLongerThanSixMonths() {
        StrategyValidationException exception = assertThrows(
                StrategyValidationException.class,
                () -> strategySimulationBoundsService.validateSimulationRequest(new StrategySimulationConfig(
                        "2024-01-01",
                        "2024-08-01",
                        1000.0,
                        true)));

        assertEquals("Simulations are limited to a maximum six-month range.", exception.getMessage());
    }

    @Test
    void rejectsSimulationDatesOutsideAvailableCoverage() {
        when(stockPriceJdbcRepository.findGlobalPriceCoverage()).thenReturn(Optional.of(
                new StockPriceJdbcRepository.PriceCoverage(
                        Instant.parse("2024-01-01T00:00:00Z"),
                        Instant.parse("2024-06-30T00:00:00Z"))));

        StrategyValidationException exception = assertThrows(
                StrategyValidationException.class,
                () -> strategySimulationBoundsService.validateSimulationRequest(new StrategySimulationConfig(
                        "2023-12-31",
                        "2024-01-31",
                        1000.0,
                        true)));

        assertEquals("Simulation dates must stay between 2024-01-01 and 2024-06-30.", exception.getMessage());
    }

    @Test
    void acceptsSimulationDatesWithinCoverageAndRange() {
        when(stockPriceJdbcRepository.findGlobalPriceCoverage()).thenReturn(Optional.of(
                new StockPriceJdbcRepository.PriceCoverage(
                        Instant.parse("2024-01-01T00:00:00Z"),
                        Instant.parse("2024-12-31T00:00:00Z"))));

        assertDoesNotThrow(() -> strategySimulationBoundsService.validateSimulationRequest(new StrategySimulationConfig(
                "2024-02-01",
                "2024-06-30",
                1000.0,
                true)));
    }
}
