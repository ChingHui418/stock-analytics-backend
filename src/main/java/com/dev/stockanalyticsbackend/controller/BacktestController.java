package com.dev.stockanalyticsbackend.controller;

import com.dev.stockanalyticsbackend.dto.BacktestResultDto;
import com.dev.stockanalyticsbackend.service.BacktestService;
import com.dev.stockanalyticsbackend.service.StockDataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {

    @Autowired
    private BacktestService backtestService;

    @Autowired
    private StockDataSyncService stockDataSyncService;

    // --- 定期定額 API ---
    // http://localhost:8080/api/backtest/basic?symbol=0050&startDate=2026-08-01&endDate=2026-09-13&amount=10000&day=5
    @GetMapping("/basic")
    public BacktestResultDto basicDcaBacktest(
            @RequestParam String symbol,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam BigDecimal amount,
            @RequestParam int day) {

        // 將網址傳進來的字串日期轉換為 LocalDate 物件
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        stockDataSyncService.ensureDataReady(symbol);

        // 呼叫我們剛剛在 BacktestService 寫好的核心邏輯
        return backtestService.calculateBasicDca(symbol, start, end, amount, day);
    }

    // --- 定期定額 + 股息再投入 (DRIP) API ---
    // http://localhost:8080/api/backtest/drip?symbol=00878&startDate=2023-01-01&endDate=2026-09-01&amount=10000&day=5
    @GetMapping("/drip")
    public BacktestResultDto dripBacktest(
            @RequestParam String symbol,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam BigDecimal amount,
            @RequestParam int day) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        stockDataSyncService.ensureDataReady(symbol);

        return backtestService.calculateDripBacktest(symbol, start, end, amount, day);
    }
}