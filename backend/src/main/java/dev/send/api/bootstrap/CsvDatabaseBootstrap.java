package dev.send.api.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CsvDatabaseBootstrap implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(CsvDatabaseBootstrap.class);
  private static final int BATCH_SIZE = 2_000;

  private static final CSVFormat CSV_FORMAT =
      CSVFormat.DEFAULT
          .builder()
          .setDelimiter(';')
          .setHeader()
          .setSkipHeaderRecord(true)
          .setAllowMissingColumnNames(true)
          .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
          .build();

  private static final String INSERT_COMPANIES_SQL =
      """
            INSERT INTO us_companies (
                ticker, company_name, industry_id, isin, fiscal_year_end_month, number_employees,
                business_summary, market, cik, main_currency
            ) VALUES (
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?
            )
            """;

  private static final String INSERT_FINANCIAL_STATEMENTS_SQL =
      """
            INSERT INTO us_financial_statements (
                source_dataset, ticker, currency, fiscal_year, fiscal_period, report_date, publish_date, restated_date,
                revenue, cost_of_revenue, sga, research_and_development, income_da, interest_expense_net,
                abnormal_gains, income_tax_net, extraordinary_gains_losses, net_income,
                shares_basic, shares_diluted, cash_and_st_investments, accounts_notes_receivables, inventories,
                total_current_assets, ppe_net, lt_investments_receivables, other_lt_assets, total_noncurrent_assets,
                total_assets, payables_accruals, st_debt, total_current_liabilities, lt_debt,
                total_noncurrent_liabilities, total_liabilities, share_capital_apic, treasury_stock,
                retained_earnings, total_equity, total_liabilities_equity,
                cf_net_income_starting_line, cf_da, change_in_fixed_assets_intangibles, change_in_working_capital,
                change_in_accounts_receivable, change_in_inventories, change_in_accounts_payable, change_in_other,
                net_cash_operating_activities, change_fixed_assets_intangibles, net_change_lti,
                net_cash_acquisitions_divestitures, net_cash_investing_activities, dividends_paid,
                repayment_of_debt, repurchase_of_equity, net_cash_financing_activities, net_change_cash
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?,
                ?, ?, ?, ?
            )
            """;

  private static final String INSERT_SHAREPRICES_SQL =
      """
            INSERT INTO stock_prices (
                symbol, time, open, high, low, close, adj_close, volume, dividend, shares_outstanding
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

  private static final List<String> INCOME_FIELDS =
      List.of(
          "revenue",
          "cost_of_revenue",
          "sga",
          "research_and_development",
          "income_da",
          "interest_expense_net",
          "abnormal_gains",
          "income_tax_net",
          "extraordinary_gains_losses",
          "net_income");

  private static final List<String> BALANCE_FIELDS =
      List.of(
          "shares_basic",
          "shares_diluted",
          "cash_and_st_investments",
          "accounts_notes_receivables",
          "inventories",
          "total_current_assets",
          "ppe_net",
          "lt_investments_receivables",
          "other_lt_assets",
          "total_noncurrent_assets",
          "total_assets",
          "payables_accruals",
          "st_debt",
          "total_current_liabilities",
          "lt_debt",
          "total_noncurrent_liabilities",
          "total_liabilities",
          "share_capital_apic",
          "treasury_stock",
          "retained_earnings",
          "total_equity",
          "total_liabilities_equity");

  private static final List<String> CASHFLOW_FIELDS =
      List.of(
          "cf_net_income_starting_line",
          "cf_da",
          "change_in_fixed_assets_intangibles",
          "change_in_working_capital",
          "change_in_accounts_receivable",
          "change_in_inventories",
          "change_in_accounts_payable",
          "change_in_other",
          "net_cash_operating_activities",
          "change_fixed_assets_intangibles",
          "net_change_lti",
          "net_cash_acquisitions_divestitures",
          "net_cash_investing_activities",
          "dividends_paid",
          "repayment_of_debt",
          "repurchase_of_equity",
          "net_cash_financing_activities",
          "net_change_cash");

  private final DatabaseBootstrapProperties properties;
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  public CsvDatabaseBootstrap(
      DatabaseBootstrapProperties properties, JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (!properties.isEnabled()) {
      log.info("Database bootstrap is disabled.");
      return;
    }

    if (!isDatabaseEmpty()) {
      log.info("Database bootstrap skipped because target tables already contain data.");
      return;
    }

    Path dataDirectory = properties.getDataDirectory().toAbsolutePath().normalize();
    if (!Files.isDirectory(dataDirectory)) {
      throw new IllegalStateException(
          "Database bootstrap data directory does not exist: " + dataDirectory);
    }

    log.info("Bootstrapping database from CSV files in {}", dataDirectory);
    loadCompanies(dataDirectory.resolve("us-companies.csv"));
    loadFinancialStatements(
        dataDirectory.resolve("us-income-quarterly.csv"), "us-income-quarterly");
    loadFinancialStatements(
        dataDirectory.resolve("us-balance-quarterly.csv"), "us-balance-quarterly");
    loadFinancialStatements(
        dataDirectory.resolve("us-cashflow-quarterly.csv"), "us-cashflow-quarterly");
    loadSharePrices(dataDirectory.resolve("us-shareprices-daily.csv"));
    log.info("Database bootstrap finished.");
  }

  private boolean isDatabaseEmpty() {
    return isTableEmpty("us_companies")
        && isTableEmpty("us_financial_statements")
        && isTableEmpty("stock_prices");
  }

  private boolean isTableEmpty(String tableName) {
    Boolean empty =
        jdbcTemplate.queryForObject(
            "SELECT NOT EXISTS (SELECT 1 FROM " + tableName + " LIMIT 1)", Boolean.class);
    return Boolean.TRUE.equals(empty);
  }

  private void loadCompanies(Path csvPath) throws IOException, SQLException {
    requireFile(csvPath);
    executeBatch(
        csvPath,
        INSERT_COMPANIES_SQL,
        (record, statement) -> {
          if (shouldSkipCompanyRecord(record)) {
            return false;
          }
          setText(statement, 1, get(record, 0));
          setText(statement, 2, get(record, 1));
          setLong(statement, 3, get(record, 2));
          setText(statement, 4, get(record, 3));
          setInteger(statement, 5, get(record, 4));
          setLong(statement, 6, get(record, 5));
          setText(statement, 7, get(record, 6));
          setText(statement, 8, get(record, 7));
          setLong(statement, 9, get(record, 8));
          setText(statement, 10, get(record, 9));
          return true;
        });
  }

  private void loadFinancialStatements(Path csvPath, String sourceDataset)
      throws IOException, SQLException {
    requireFile(csvPath);
    executeBatch(
        csvPath,
        INSERT_FINANCIAL_STATEMENTS_SQL,
        (record, statement) -> {
          int parameterIndex = 1;
          setText(statement, parameterIndex++, sourceDataset);
          setText(statement, parameterIndex++, get(record, "ticker"));
          setText(statement, parameterIndex++, get(record, "currency"));
          setInteger(statement, parameterIndex++, get(record, "fiscal_year"));
          setText(statement, parameterIndex++, get(record, "fiscal_period"));
          setDate(statement, parameterIndex++, get(record, "report_date"));
          setDate(statement, parameterIndex++, get(record, "publish_date"));
          setDate(statement, parameterIndex++, get(record, "restated_date"));

          parameterIndex =
              setOptionalDoubleFields(statement, record, parameterIndex, INCOME_FIELDS);
          parameterIndex =
              setOptionalDoubleFields(statement, record, parameterIndex, BALANCE_FIELDS);
          parameterIndex =
              setOptionalDoubleFields(statement, record, parameterIndex, CASHFLOW_FIELDS);
          return true;
        });
  }

  private void loadSharePrices(Path csvPath) throws IOException, SQLException {
    requireFile(csvPath);
    executeBatch(
        csvPath,
        INSERT_SHAREPRICES_SQL,
        (record, statement) -> {
          setText(statement, 1, get(record, "ticker"));
          setTimestamp(statement, 2, get(record, "price_date"));
          setDouble(statement, 3, get(record, "open"));
          setDouble(statement, 4, get(record, "high"));
          setDouble(statement, 5, get(record, "low"));
          setDouble(statement, 6, get(record, "close"));
          setDouble(statement, 7, get(record, "adj_close"));
          setLong(statement, 8, get(record, "volume"));
          setDouble(statement, 9, get(record, "dividend"));
          setLong(statement, 10, get(record, "shares_outstanding"));
          return true;
        });
  }

  private int setOptionalDoubleFields(
      PreparedStatement statement, CSVRecord record, int startIndex, List<String> fieldNames)
      throws SQLException {
    int parameterIndex = startIndex;
    for (String fieldName : fieldNames) {
      setDouble(statement, parameterIndex++, get(record, fieldName));
    }
    return parameterIndex;
  }

  private void executeBatch(Path csvPath, String sql, StatementBinder binder)
      throws IOException, SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql);
        BufferedReader reader = Files.newBufferedReader(csvPath);
        CSVParser parser = CSV_FORMAT.parse(reader)) {
      connection.setAutoCommit(false);
      int pending = 0;
      int total = 0;
      for (CSVRecord record : parser) {
        if (!binder.bind(record, statement)) {
          continue;
        }
        statement.addBatch();
        pending++;
        total++;
        if (pending == BATCH_SIZE) {
          statement.executeBatch();
          connection.commit();
          pending = 0;
        }
      }
      if (pending > 0) {
        statement.executeBatch();
        connection.commit();
      }
      log.info("Loaded {} rows from {}", total, csvPath.getFileName());
    } catch (IOException | SQLException exception) {
      log.error("Failed while loading {}", csvPath, exception);
      throw exception;
    }
  }

  private void requireFile(Path csvPath) {
    if (!Files.isRegularFile(csvPath)) {
      throw new IllegalStateException("Expected bootstrap CSV file was not found: " + csvPath);
    }
  }

  @Nullable
  private String get(CSVRecord record, int index) {
    return normalize(record.get(index));
  }

  @Nullable
  private String get(CSVRecord record, String name) {
    return record.isMapped(name) ? normalize(record.get(name)) : null;
  }

  private boolean shouldSkipCompanyRecord(CSVRecord record) {
    String ticker = get(record, 0);
    if (ticker == null || ticker.isBlank()) {
      log.warn("Skipping malformed us-companies row {}", record.getRecordNumber() + 1);
      return true;
    }

    String industryId = get(record, 2);
    if (industryId != null && !industryId.chars().allMatch(Character::isDigit)) {
      log.warn(
          "Skipping malformed us-companies row {} due to non-numeric industry_id '{}'",
          record.getRecordNumber() + 1,
          industryId);
      return true;
    }

    return false;
  }

  @Nullable
  private String normalize(String value) {
    return Objects.requireNonNullElse(value, "").trim().isEmpty() ? null : value.trim();
  }

  private void setText(PreparedStatement statement, int parameterIndex, @Nullable String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(parameterIndex, Types.VARCHAR);
      return;
    }
    statement.setString(parameterIndex, value);
  }

  private void setInteger(PreparedStatement statement, int parameterIndex, @Nullable String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(parameterIndex, Types.INTEGER);
      return;
    }
    statement.setInt(parameterIndex, Integer.parseInt(value));
  }

  private void setLong(PreparedStatement statement, int parameterIndex, @Nullable String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(parameterIndex, Types.BIGINT);
      return;
    }
    statement.setLong(parameterIndex, Long.parseLong(value));
  }

  private void setDouble(PreparedStatement statement, int parameterIndex, @Nullable String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(parameterIndex, Types.DOUBLE);
      return;
    }
    statement.setDouble(parameterIndex, Double.parseDouble(value));
  }

  private void setDate(PreparedStatement statement, int parameterIndex, @Nullable String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(parameterIndex, Types.DATE);
      return;
    }
    statement.setDate(parameterIndex, Date.valueOf(value));
  }

  private void setTimestamp(PreparedStatement statement, int parameterIndex, @Nullable String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(parameterIndex, Types.TIMESTAMP_WITH_TIMEZONE);
      return;
    }
    statement.setObject(parameterIndex, OffsetDateTime.parse(value + "T00:00:00Z"));
  }

  @FunctionalInterface
  private interface StatementBinder {
    boolean bind(CSVRecord record, PreparedStatement statement) throws SQLException;
  }
}
