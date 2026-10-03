package com.dev.stockanalyticsbackend.scheduler;

import com.dev.stockanalyticsbackend.entity.StockEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dev.stockanalyticsbackend.repository.StockRepository;
import com.dev.stockanalyticsbackend.service.TwseCrawlerService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CrawlerScheduler {

    @Autowired
    private TwseCrawlerService twseCrawlerService;

    @Autowired
    private StockRepository stockRepository;

    @Scheduled(cron = "0 0 15 * * MON-FRI")
    public void runDailyTwseCrawler() {
        System.out.println("啟動每日爬蟲排程...");

        // 取得當天的日期格式 (如 "20260913")
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 從資料庫撈出我們所有關注的股票，一檔一檔抓
        List<StockEntity> allStocks = stockRepository.findAll();
        for (StockEntity stock : allStocks) {
            twseCrawlerService.fetchMonthPrices(stock.getSymbol(), todayStr);

            // 避免被 TWSE 證交所當作惡意攻擊封鎖 IP，每次抓取後暫停 3 秒
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("每日爬蟲排程執行完畢！");
    }
}
