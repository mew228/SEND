package dev.send.api.marketdata.infra.persistence;

import dev.send.api.marketdata.domain.DailyStockPrice;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StockPriceJdbcRepository {
  public static final String UPSERT_SQL =
      """
            INSERT INTO stock_prices (symbol, time, open, high, low, close, volume)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (symbol, time) DO UPDATE
            SET open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                volume = EXCLUDED.volume
            """;

  private static final String FIND_LATEST_TIME_SQL =
      """
            SELECT MAX(time)
            FROM stock_prices
            WHERE symbol = ?
            """;

  private static final String FIND_GLOBAL_PRICE_COVERAGE_SQL =
      """
            SELECT MIN(time) AS earliest_time, MAX(time) AS latest_time
            FROM stock_prices
            """;

  private final JdbcTemplate jdbcTemplate;

  public StockPriceJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int upsert(DailyStockPrice stockPrice) {
    return jdbcTemplate.update(
        UPSERT_SQL,
        stockPrice.symbol(),
        Timestamp.from(stockPrice.time()),
        stockPrice.open(),
        stockPrice.high(),
        stockPrice.low(),
        stockPrice.close(),
        stockPrice.volume());
  }

  public Optional<Instant> findLatestTime(String symbol) {
    return Optional.ofNullable(
            jdbcTemplate.queryForObject(FIND_LATEST_TIME_SQL, Timestamp.class, symbol))
        .map(Timestamp::toInstant);
  }

  public Optional<PriceCoverage> findGlobalPriceCoverage() {
    Map<String, Object> row = jdbcTemplate.queryForMap(FIND_GLOBAL_PRICE_COVERAGE_SQL);
    Object earliest = row.get("earliest_time");
    Object latest = row.get("latest_time");
    if (!(earliest instanceof Timestamp earliestTimestamp)
        || !(latest instanceof Timestamp latestTimestamp)) {
      return Optional.empty();
    }

    return Optional.of(
        new PriceCoverage(earliestTimestamp.toInstant(), latestTimestamp.toInstant()));
  }

  public record PriceCoverage(Instant earliestTime, Instant latestTime) {}
}
