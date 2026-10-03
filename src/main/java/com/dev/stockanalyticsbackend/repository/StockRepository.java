package com.dev.stockanalyticsbackend.repository;

import com.dev.stockanalyticsbackend.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, Long> {
    // 自定義查詢：透過股票代號 (symbol) 來尋找股票                                                                 SQL: SELECT * FROM stocks WHERE symbol = ?
    Optional<StockEntity> findBySymbol(String symbol);
}
