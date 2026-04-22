package dev.send.api.strategy.application;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import dev.send.api.marketdata.infra.persistence.StockPriceJdbcRepository;
import dev.send.api.worker.application.StrategySimulationConfig;

@Service
public class StrategySimulationBoundsService {
    private static final int MAX_SIMULATION_MONTHS = 6;
    private static final int DEFAULT_LECTURE_SIMULATION_DAYS = 30;

    private final StockPriceJdbcRepository stockPriceJdbcRepository;

  public StrategySimulationBoundsService(StockPriceJdbcRepository stockPriceJdbcRepository) {
    this.stockPriceJdbcRepository = stockPriceJdbcRepository;
  }

    public StrategySimulationBounds getSimulationBounds() {
        return stockPriceJdbcRepository.findGlobalPriceCoverage()
                .map(coverage -> new StrategySimulationBounds(
                        true,
                        coverage.earliestTime().atOffset(ZoneOffset.UTC).toLocalDate().toString(),
                        coverage.latestTime().atOffset(ZoneOffset.UTC).toLocalDate().toString()))
                .orElseGet(() -> new StrategySimulationBounds(false, null, null));
    }

    public void validateSimulationRequest(StrategySimulationConfig simulationConfig) {
        LocalDate startDate = parseIsoDate(simulationConfig.startDate(), "startDate");
        LocalDate endDate = parseIsoDate(simulationConfig.endDate(), "endDate");
        if (endDate.isBefore(startDate)) {
            throw new StrategyValidationException("Simulation endDate must be on or after startDate.");
        }
        if (endDate.isAfter(startDate.plusMonths(MAX_SIMULATION_MONTHS))) {
            throw new StrategyValidationException(
                    "Simulations are limited to a maximum six-month range.");
        }

        StrategySimulationBounds bounds = getSimulationBounds();
        if (!bounds.hasPriceData() || bounds.earliestPriceDate() == null || bounds.latestPriceDate() == null) {
            throw new StrategyValidationException("Simulation is unavailable until stock price data is loaded.");
        }

        LocalDate earliestDate = parseIsoDate(bounds.earliestPriceDate(), "earliestPriceDate");
        LocalDate latestDate = parseIsoDate(bounds.latestPriceDate(), "latestPriceDate");
        if (startDate.isBefore(earliestDate) || endDate.isAfter(latestDate)) {
            throw new StrategyValidationException(
                    "Simulation dates must stay between " + earliestDate + " and " + latestDate + ".");
        }
    }

    public StrategySimulationConfig createLectureSimulationConfig(double initialCash, boolean includeTrace) {
        StrategySimulationBounds bounds = getSimulationBounds();
        if (!bounds.hasPriceData() || bounds.earliestPriceDate() == null || bounds.latestPriceDate() == null) {
            throw new StrategyValidationException("Simulation is unavailable until stock price data is loaded.");
        }

        LocalDate earliestDate = parseIsoDate(bounds.earliestPriceDate(), "earliestPriceDate");
        LocalDate latestDate = parseIsoDate(bounds.latestPriceDate(), "latestPriceDate");
        LocalDate startDate = latestDate.minusDays(DEFAULT_LECTURE_SIMULATION_DAYS - 1L);
        if (startDate.isBefore(earliestDate)) {
            startDate = earliestDate;
        }

        StrategySimulationConfig simulationConfig = new StrategySimulationConfig(
                startDate.toString(),
                latestDate.toString(),
                initialCash,
                includeTrace);
        validateSimulationRequest(simulationConfig);
        return simulationConfig;
    }

    private LocalDate parseIsoDate(String rawDate, String fieldName) {
        try {
            return LocalDate.parse(rawDate);
        } catch (DateTimeParseException exception) {
            throw new StrategyValidationException(
                    "Simulation " + fieldName + " must be a valid ISO-8601 date.");
        }
    }
}
