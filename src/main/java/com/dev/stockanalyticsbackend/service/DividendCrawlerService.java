package com.dev.stockanalyticsbackend.service;

import com.dev.stockanalyticsbackend.entity.DividendEntity;
import com.dev.stockanalyticsbackend.entity.StockEntity;
import com.dev.stockanalyticsbackend.repository.DividendRepository;
import com.dev.stockanalyticsbackend.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Service
public class DividendCrawlerService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private DividendRepository dividendRepository;

    /**
     * 從 Yahoo Finance 爬取該檔股票上市以來的所有配息紀錄
     * @param symbol 台股代號 (如 "00878", "0050")
     */
    public void fetchHistoricalDividends(String symbol) {
        Optional<StockEntity> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            System.out.println("資料庫找不到股票代號：" + symbol);
            return;
        }
        StockEntity stock = stockOpt.get();

        // 組合 Yahoo Finance 的 CSV 下載網址
        // 台股代號後面必須加上 ".TW" (上市) 才能讓 Yahoo 辨識
        // period1=0 代表從 1970 年開始，period2=2000000000 代表抓到未來，events=div 代表只抓配息
        // 改用 v8 的 chart JSON API 端點
        String yahooSymbol = symbol + ".TW";
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol
                + "?period1=0&period2=2000000000&interval=1d&events=div";

        try {
            // 建立 HttpHeaders，偽裝成一般的 Windows Chrome 瀏覽器
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            // 將 headers 包裝進 HttpEntity
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            // restTemplate.exchange 發送帶有 Header 的 GET 請求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    httpEntity, // <-- 這就是裝著 Chrome User-Agent 的包裹
                    String.class
            );

            String jsonData = response.getBody();
            if (jsonData == null || jsonData.isEmpty()) return;

            // 使用 Jackson 解析 JSON 樹狀結構
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonData);

            // 深入尋找 JSON 裡的 dividends 節點: chart -> result[0] -> events -> dividends
            JsonNode resultNode = root.path("chart").path("result").get(0);
            if (resultNode == null || resultNode.isMissingNode()) return;

            JsonNode dividendsNode = resultNode.path("events").path("dividends");
            if (dividendsNode.isMissingNode()) {
                System.out.println(symbol + " 目前無配息紀錄。");
                return;
            }

            // dividendsNode 直接反序列化為 Java 的 Map
            Map<String, Map<String, Object>> dividendsMap = mapper.readValue(
                    dividendsNode.toString(), Map.class
            );

            int saveCount = 0;
            // 使用 Java 內建最穩定的 Map.Entry 來跑迴圈
            for (Map.Entry<String, Map<String, Object>> entry : dividendsMap.entrySet()) {
                String timestampStr = entry.getKey();
                Map<String, Object> divData = entry.getValue();

                long timestamp = Long.parseLong(timestampStr);
                LocalDate exDate = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();

                // 從 Map 中取出 amount 並轉換為 BigDecimal
                BigDecimal dividendAmount = new BigDecimal(divData.get("amount").toString()).setScale(4, RoundingMode.HALF_UP);

                if (!dividendRepository.existsByStockIdAndExDividendDate(stock.getId(), exDate)) {
                    DividendEntity dividendEntity = new DividendEntity();
                    dividendEntity.setStock(stock);
                    dividendEntity.setExDividendDate(exDate);
                    dividendEntity.setAnnounceDate(exDate);
                    dividendEntity.setCashDividend(dividendAmount);
                    dividendEntity.setStockDividend(BigDecimal.ZERO);

                    dividendRepository.save(dividendEntity);
                    saveCount++;
                }
            }
            System.out.println(symbol + " 歷史配息抓取完畢，共新增 " + saveCount + " 筆除息紀錄！");

        } catch (Exception e) {
            System.err.println("抓取 " + symbol + " 配息時發生錯誤。");
            e.printStackTrace();
        }
    }
}
