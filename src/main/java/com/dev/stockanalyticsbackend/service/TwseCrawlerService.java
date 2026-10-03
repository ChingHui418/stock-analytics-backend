package com.dev.stockanalyticsbackend.service;

import com.dev.stockanalyticsbackend.entity.DailyPriceEntity;
import com.dev.stockanalyticsbackend.entity.StockEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.dev.stockanalyticsbackend.repository.DailyPriceRepository;
import com.dev.stockanalyticsbackend.repository.StockRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class TwseCrawlerService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot 內建的 JSON 解析器

    /**
     * 爬取指定股票、指定月份的每日價格
     * @param symbol 股票代號 (如 "0050", "2892", "00891")
     * @param dateStr 查詢月份，格式為 yyyyMMdd (如 "20260901")
     */
    public void fetchMonthPrices(String symbol, String dateStr) {
        // 確認該股票是否存在於資料庫內
        Optional<StockEntity> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) {
            System.out.println("資料庫找不到股票代號：" + symbol + "，請先建立股票基本資料。");
            return;
        }
        StockEntity stock = stockOpt.get();

        // 組合 TWSE API 網址
        String url = "https://www.twse.com.tw/exchangeReport/STOCK_DAY?response=json&date=" + dateStr + "&stockNo=" + symbol;

        try {
            // 發送 HTTP GET 請求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String jsonBody = response.getBody();

            // 解析 JSON
            JsonNode root = objectMapper.readTree(jsonBody);

            // TWSE 的狀態碼，"OK" 代表有資料
            if (!"OK".equals(root.get("stat").asText())) {
                System.out.println(symbol + " 該月份無資料或查無此代號");
                return;
            }

            // 取得 "data" 陣列 (裡面包含每日的價格資訊)
            JsonNode dataArray = root.get("data");
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode dailyData : dataArray) {
                    // TWSE 欄位索引：
                    // [0]日期(115/09/01), [1]成交股數, [2]成交金額, [3]開盤價, [4]最高價, [5]最低價, [6]收盤價

                    String twseDate = dailyData.get(0).asText();
                    LocalDate tradeDate = parseTwseDate(twseDate);

                    // 檢查這天的資料是否已經存過，避免重複寫入
                    if (dailyPriceRepository.existsByStockIdAndTradeDate(stock.getId(), tradeDate)) {
                        continue;
                    }

                    // 將包含逗號的字串 (如 "1,234.56") 清理後轉為數值
                    String openStr = dailyData.get(3).asText().replace(",", "");
                    String closeStr = dailyData.get(6).asText().replace(",", "");
                    String volumeStr = dailyData.get(1).asText().replace(",", "");

                    // 有些日期的股票可能沒有交易 (數值為 "--")，需過濾
                    if ("--".equals(openStr) || "--".equals(closeStr)) {
                        continue;
                    }

                    // 建立並儲存 DailyPriceEntity
                    DailyPriceEntity priceEntity = new DailyPriceEntity();
                    priceEntity.setStock(stock);
                    priceEntity.setTradeDate(tradeDate);
                    priceEntity.setOpenPrice(new BigDecimal(openStr));
                    priceEntity.setClosePrice(new BigDecimal(closeStr));
                    priceEntity.setVolume(Long.parseLong(volumeStr));

                    dailyPriceRepository.save(priceEntity);
                }
                System.out.println(symbol + " 於 " + dateStr + " 的股價資料爬取並儲存成功！");
            }

        } catch (Exception e) {
            System.err.println("爬取過程發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 處理證交所的民國年日期格式 (例如將 "115/09/01" 轉為 2026-09-01)
     */
    private LocalDate parseTwseDate(String twseDate) {
        String[] parts = twseDate.split("/");
        int rocYear = Integer.parseInt(parts[0]);
        int gregorianYear = rocYear + 1911; // 民國年轉西元年

        String standardDateStr = gregorianYear + "-" + parts[1] + "-" + parts[2];
        return LocalDate.parse(standardDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
