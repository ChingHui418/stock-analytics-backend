package com.dev.stockanalyticsbackend.controller;

import com.dev.stockanalyticsbackend.service.DividendCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dev.stockanalyticsbackend.service.TwseCrawlerService;

@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    @Autowired
    private TwseCrawlerService twseCrawlerService;

    @Autowired
    private DividendCrawlerService dividendCrawlerService;

    // ---「歷史股價」---
    @GetMapping("/trigger")
    public String triggerCrawler(@RequestParam String symbol, @RequestParam String date) {
        twseCrawlerService.fetchMonthPrices(symbol, date);
        return "歷史股價已觸發，(股票：" + symbol + ", 月份：" + date + ")";
    }

    // ---「歷史配息」---
    @GetMapping("/trigger-dividend")
    public String triggerDividendCrawler(@RequestParam String symbol) {
        dividendCrawlerService.fetchHistoricalDividends(symbol);
        return "歷史配息爬蟲已觸發，(股票：" + symbol + ")";
    }
}
