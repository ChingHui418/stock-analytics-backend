package com.dev.stockanalyticsbackend.service;

import com.dev.stockanalyticsbackend.entity.DailyPriceEntity;
import com.dev.stockanalyticsbackend.entity.StockEntity;
import com.dev.stockanalyticsbackend.repository.DailyPriceRepository;
import com.dev.stockanalyticsbackend.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StockDataSyncService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private DividendCrawlerService dividendCrawlerService;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 攔截回測請求，確保資料庫已經有這檔股票的資料
     */
    public void ensureDataReady(String symbol) {
        // 1. 自動建檔：如果資料庫沒有這檔股票，就自動新增
        StockEntity stock = stockRepository.findBySymbol(symbol).orElseGet(() -> {
            System.out.println("偵測到新股票 " + symbol + "，正在自動建檔...");
            StockEntity newStock = new StockEntity();
            newStock.setSymbol(symbol);
            newStock.setName(symbol);
            newStock.setAssetType("STOCK");
            return stockRepository.save(newStock);
        });

        // 2. 檢查是否已經有股價資料
        // 隨便查一個極大的區間，看看這檔股票是不是空的
        List<DailyPriceEntity> checkPrices = dailyPriceRepository.findByStockIdAndTradeDateBetweenOrderByTradeDateAsc(
                stock.getId(), LocalDate.of(2000, 1, 1), LocalDate.now()
        );

        // 如果連一筆股價都沒有，代表是一檔全新的股票，啟動 Yahoo 自動補抓機制
        if (checkPrices.isEmpty()) {
            System.out.println("準備自動下載 " + symbol + " 的歷史股價與配息資料...");

            // 抓取上市以來的全部股價
            fetchAllHistoricalPricesFromYahoo(stock);

            // 抓取上市以來的全部配息
            dividendCrawlerService.fetchHistoricalDividends(symbol);

            System.out.println(symbol + " 所有歷史數據自動同步完成！");
        }
    }

    /**
     * 利用 Yahoo API 一秒抓取 20 年歷史股價
     */
    private void fetchAllHistoricalPricesFromYahoo(StockEntity stock) {
        // 網址不帶 events=div，預設就是回傳每日歷史股價
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + stock.getSymbol() + ".TW?period1=0&period2=2000000000&interval=1d";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            String jsonData = response.getBody();
            if (jsonData == null || jsonData.isEmpty()) return;

            ObjectMapper mapper = new ObjectMapper();

            // 使用 Map 強制轉型大法解析 JSON，避開底層相容性問題
            Map<String, Object> root = mapper.readValue(jsonData, Map.class);
            Map<String, Object> chart = (Map<String, Object>) root.get("chart");
            List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");

            if (result == null || result.isEmpty()) return;

            Map<String, Object> result0 = result.get(0);
            List<Number> timestamps = (List<Number>) result0.get("timestamp");
            Map<String, Object> indicators = (Map<String, Object>) result0.get("indicators");
            List<Map<String, Object>> quote = (List<Map<String, Object>>) indicators.get("quote");
            List<Number> closes = (List<Number>) quote.get(0).get("close");

            // 準備批次存入資料庫
            List<DailyPriceEntity> entitiesToSave = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                if (closes.get(i) == null) continue; // 跳過沒有開盤或沒有收盤價的日子

                long ts = timestamps.get(i).longValue();
                LocalDate date = Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate();
                BigDecimal closePrice = new BigDecimal(closes.get(i).toString()).setScale(2, RoundingMode.HALF_UP);

                DailyPriceEntity priceEntity = new DailyPriceEntity();
                priceEntity.setStock(stock);
                priceEntity.setTradeDate(date);
                priceEntity.setClosePrice(closePrice);
                priceEntity.setVolume(0L); // 成交量暫時不需要，塞0

                entitiesToSave.add(priceEntity);
            }

            dailyPriceRepository.saveAll(entitiesToSave);
            System.out.println(stock.getSymbol() + " 歷史股價抓取完畢，共新增 " + entitiesToSave.size() + " 筆紀錄！");

        } catch (Exception e) {
            System.err.println("抓取 " + stock.getSymbol() + " 股價時發生錯誤。");
            e.printStackTrace();
        }
    }
}
