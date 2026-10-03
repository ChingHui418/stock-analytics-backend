package com.dev.stockanalyticsbackend.repository;
import com.dev.stockanalyticsbackend.entity.DailyPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPriceEntity, Long> {

    // 回測時，需要撈出某檔股票從 A 日期 到 B 日期 的所有價格，並依日期排序
    // 相當於 SQL: SELECT * FROM daily_prices WHERE stock_id = ? AND trade_date BETWEEN ? AND ? ORDER BY trade_date ASC
    List<DailyPriceEntity> findByStockIdAndTradeDateBetweenOrderByTradeDateAsc(Long stockId, LocalDate startDate, LocalDate endDate);

    // 確保不會重複寫入同一天的股價資料
    boolean existsByStockIdAndTradeDate(Long stockId, LocalDate tradeDate);

}
