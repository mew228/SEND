package dev.send.api.marketdata.infra.persistence;

import dev.send.api.marketdata.domain.StockFundamentals;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stock_fundamentals")
public class StockFundamentalsEntity {
  @Id
  @Column(name = "symbol", nullable = false)
  private String symbol = "";

  @Nullable
  @Column(name = "eps")
  private Double eps;

  @Nullable
  @Column(name = "pe_ratio")
  private Double peRatio;

  @Nullable
  @Column(name = "beta")
  private Double beta;

  @Nullable
  @Column(name = "as_of_date")
  private LocalDate asOfDate;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_metrics", nullable = false)
  private Map<String, Double> extraMetrics = Map.of();

  @Column(name = "refreshed_at", nullable = false)
  private Instant refreshedAt = Instant.EPOCH;

  protected StockFundamentalsEntity() {}

  public StockFundamentalsEntity(
      String symbol,
      @Nullable Double eps,
      @Nullable Double peRatio,
      @Nullable Double beta,
      @Nullable LocalDate asOfDate,
      Map<String, Double> extraMetrics,
      Instant refreshedAt) {
    this.symbol = Objects.requireNonNull(symbol, "symbol must not be null");
    this.eps = eps;
    this.peRatio = peRatio;
    this.beta = beta;
    this.asOfDate = asOfDate;
    this.extraMetrics = Map.copyOf(extraMetrics);
    this.refreshedAt = Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
  }

  public static StockFundamentalsEntity fromDomain(StockFundamentals stockFundamentals) {
    return new StockFundamentalsEntity(
        stockFundamentals.symbol(),
        stockFundamentals.eps(),
        stockFundamentals.peRatio(),
        stockFundamentals.beta(),
        stockFundamentals.asOfDate(),
        stockFundamentals.extraMetrics(),
        stockFundamentals.refreshedAt());
  }

  public StockFundamentals toDomain() {
    return new StockFundamentals(symbol, eps, peRatio, beta, asOfDate, extraMetrics, refreshedAt);
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = Objects.requireNonNull(symbol, "symbol must not be null");
  }

  @Nullable
  public Double getEps() {
    return eps;
  }

  public void setEps(@Nullable Double eps) {
    this.eps = eps;
  }

  @Nullable
  public Double getPeRatio() {
    return peRatio;
  }

  public void setPeRatio(@Nullable Double peRatio) {
    this.peRatio = peRatio;
  }

  @Nullable
  public Double getBeta() {
    return beta;
  }

  public void setBeta(@Nullable Double beta) {
    this.beta = beta;
  }

  @Nullable
  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public void setAsOfDate(@Nullable LocalDate asOfDate) {
    this.asOfDate = asOfDate;
  }

  public Map<String, Double> getExtraMetrics() {
    return extraMetrics;
  }

  public void setExtraMetrics(Map<String, Double> extraMetrics) {
    this.extraMetrics = Map.copyOf(extraMetrics);
  }

  public Instant getRefreshedAt() {
    return refreshedAt;
  }

  public void setRefreshedAt(Instant refreshedAt) {
    this.refreshedAt = Objects.requireNonNull(refreshedAt, "refreshedAt must not be null");
  }
}
