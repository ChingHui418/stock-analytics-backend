package com.dev.stockanalyticsbackend.repository;
import com.dev.stockanalyticsbackend.entity.DividendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DividendRepository extends JpaRepository<DividendEntity, Long> {

    // 回測計算「股息再投入」時，找出某檔股票在特定區間內的除息紀錄，依日期排序
    // 相當於 SQL: SELECT * FROM dividends WHERE stock_id = ? AND ex_dividend_date BETWEEN ? AND ? ORDER BY ex_dividend_date ASC
    List<DividendEntity> findByStockIdAndExDividendDateBetweenOrderByExDividendDateAsc(Long stockId, LocalDate startDate, LocalDate endDate);

    // 為了防止爬蟲重複寫入同一筆配息公告
    boolean existsByStockIdAndExDividendDate(Long stockId, LocalDate exDividendDate);
}
