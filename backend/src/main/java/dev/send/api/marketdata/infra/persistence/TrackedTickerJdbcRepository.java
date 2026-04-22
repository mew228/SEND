package dev.send.api.marketdata.infra.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TrackedTickerJdbcRepository {
  private static final String SELECT_ALL_SQL =
      """
            SELECT symbol, enabled
            FROM tracked_tickers
            ORDER BY symbol
            """;

  private static final String SELECT_ENABLED_SQL =
      """
            SELECT symbol
            FROM tracked_tickers
            WHERE enabled = TRUE
            ORDER BY symbol
            """;

  private static final String INSERT_IF_ABSENT_SQL =
      """
            INSERT INTO tracked_tickers (symbol, enabled, created_at, updated_at)
            VALUES (?, TRUE, ?, ?)
            ON CONFLICT (symbol) DO NOTHING
            """;

  private static final String UPSERT_SQL =
      """
            INSERT INTO tracked_tickers (symbol, enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (symbol) DO UPDATE
            SET enabled = EXCLUDED.enabled,
                updated_at = EXCLUDED.updated_at
            """;

  private static final String UPDATE_ENABLED_SQL =
      """
            UPDATE tracked_tickers
            SET enabled = ?, updated_at = ?
            WHERE symbol = ?
            """;

  private static final String DELETE_SQL =
      """
            DELETE FROM tracked_tickers
            WHERE symbol = ?
            """;

  private final JdbcTemplate jdbcTemplate;

  public TrackedTickerJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<TrackedTicker> findAll() {
    return jdbcTemplate.query(SELECT_ALL_SQL, this::mapTrackedTicker);
  }

  public List<String> findEnabledSymbols() {
    return jdbcTemplate.query(SELECT_ENABLED_SQL, (rs, rowNum) -> rs.getString("symbol"));
  }

  public void insertIfAbsent(String symbol) {
    Instant now = Instant.now();
    Timestamp timestamp = Timestamp.from(now);
    jdbcTemplate.update(INSERT_IF_ABSENT_SQL, symbol, timestamp, timestamp);
  }

  public void upsert(String symbol, boolean enabled) {
    Instant now = Instant.now();
    Timestamp timestamp = Timestamp.from(now);
    jdbcTemplate.update(UPSERT_SQL, symbol, enabled, timestamp, timestamp);
  }

  public void updateEnabled(String symbol, boolean enabled) {
    jdbcTemplate.update(UPDATE_ENABLED_SQL, enabled, Timestamp.from(Instant.now()), symbol);
  }

  public void delete(String symbol) {
    jdbcTemplate.update(DELETE_SQL, symbol);
  }

  private TrackedTicker mapTrackedTicker(ResultSet resultSet, int rowNum) throws SQLException {
    return new TrackedTicker(resultSet.getString("symbol"), resultSet.getBoolean("enabled"));
  }

  public record TrackedTicker(String symbol, boolean enabled) {}
}
