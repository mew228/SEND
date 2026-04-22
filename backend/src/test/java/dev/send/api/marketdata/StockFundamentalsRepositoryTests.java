package dev.send.api.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.send.api.marketdata.infra.persistence.StockFundamentalsEntity;
import dev.send.api.marketdata.infra.persistence.StockFundamentalsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class StockFundamentalsRepositoryTests {
  @Autowired private StockFundamentalsRepository stockFundamentalsRepository;

  @Test
  void saveReusesPrimaryKeyForFundamentalsUpsert() {
    stockFundamentalsRepository.saveAndFlush(
        new StockFundamentalsEntity(
            "AAPL",
            6.12,
            18.2,
            1.04,
            LocalDate.parse("2026-03-20"),
            Map.of("dividend_yield", 1.7),
            Instant.parse("2026-03-24T08:00:00Z")));

    stockFundamentalsRepository.saveAndFlush(
        new StockFundamentalsEntity(
            "AAPL",
            6.50,
            19.1,
            1.01,
            LocalDate.parse("2026-03-24"),
            Map.of("dividend_yield", 1.9, "roe", 14.2),
            Instant.parse("2026-03-24T12:00:00Z")));

    StockFundamentalsEntity entity = stockFundamentalsRepository.findById("AAPL").orElseThrow();

    assertEquals(1, stockFundamentalsRepository.count());
    assertEquals(Double.valueOf(6.50), entity.getEps());
    assertEquals(Double.valueOf(19.1), entity.getPeRatio());
    assertEquals(Double.valueOf(1.01), entity.getBeta());
    assertEquals(LocalDate.parse("2026-03-24"), entity.getAsOfDate());
    assertEquals(Double.valueOf(1.9), entity.getExtraMetrics().get("dividend_yield"));
    assertEquals(Double.valueOf(14.2), entity.getExtraMetrics().get("roe"));
  }
}
