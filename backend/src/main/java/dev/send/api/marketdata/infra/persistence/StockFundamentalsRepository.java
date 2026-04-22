package dev.send.api.marketdata.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockFundamentalsRepository
    extends JpaRepository<StockFundamentalsEntity, String> {}
