package com.dev.stockanalyticsbackend.service;

import com.dev.stockanalyticsbackend.dto.BacktestResultDto;
import com.dev.stockanalyticsbackend.entity.DailyPriceEntity;
import com.dev.stockanalyticsbackend.entity.DividendEntity;
import com.dev.stockanalyticsbackend.entity.StockEntity;
import com.dev.stockanalyticsbackend.repository.DailyPriceRepository;
import com.dev.stockanalyticsbackend.repository.DividendRepository;
import com.dev.stockanalyticsbackend.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class BacktestService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private DividendRepository dividendRepository;

    /**
     * 計算基礎定期定額 (DCA) - 不含股息再投入
     * @param symbol 股票代號
     * @param startDate 回測開始日
     * @param endDate 回測結束日
     * @param monthlyAmount 每月扣款金額
     * @param deductionDay 每月扣款日 (如 5 號)
     */
    public BacktestResultDto calculateBasicDca(String symbol, LocalDate startDate, LocalDate endDate,
                                               BigDecimal monthlyAmount, int deductionDay) {
        BacktestResultDto result = new BacktestResultDto();
        result.setSymbol(symbol);

        // 1. 確認股票是否存在
        Optional<StockEntity> stockOpt = stockRepository.findBySymbol(symbol);
        if(stockOpt.isEmpty()) {
            throw new RuntimeException("找不到股票代號：" + symbol);
        }
        StockEntity stock = stockOpt.get();

        // 2. 撈取這段期間的所有歷史股價 (資料庫已在 Repository 設定依日期遞增排序)
        List<DailyPriceEntity> priceList = dailyPriceRepository
                .findByStockIdAndTradeDateBetweenOrderByTradeDateAsc(stock.getId(), startDate, endDate);
        if (priceList.isEmpty()) {
            return result;
        }

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalShares = BigDecimal.ZERO;

        // 3. 以「月」為單位往前推進迴圈 (使用 YearMonth 可以自動處理閏年與大小月)
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(endMonth)) {
            // 決定當月的目標扣款日
            int vailDay = Math.min(deductionDay, currentMonth.lengthOfMonth());
            LocalDate targetDate = currentMonth.atDay(vailDay);

            // 4. 尋找「大於等於」目標扣款日的第一個營業日
            DailyPriceEntity buyPriceEntity = null;
            for (DailyPriceEntity dailyPriceEntity : priceList) {
                // 條件：日期不能早於目標日，且必須是同一個月份的交易日
                if(!dailyPriceEntity.getTradeDate().isBefore(targetDate) &&
                        YearMonth.from(dailyPriceEntity.getTradeDate()).equals(currentMonth)) {
                    buyPriceEntity = dailyPriceEntity; // 找到當月扣款日後
                    break;
                }
            }

            // 5. 執行買入計算
            if (buyPriceEntity != null && buyPriceEntity.getClosePrice() != null) {
                BigDecimal currentPrice = buyPriceEntity.getClosePrice();

                // 買入股數 = 扣款金額 / 收盤單價
                // 必須設定四捨五入與小數點位數，否則除不盡時會噴 ArithmeticException
                BigDecimal sharesBought = monthlyAmount.divide(currentPrice, 4, RoundingMode.HALF_UP);

                totalInvested = totalInvested.add(monthlyAmount);
                totalShares = totalShares.add(sharesBought);

                System.out.println("扣款日: " + buyPriceEntity.getTradeDate() + " | 買入價: " + currentPrice + " | 獲得股數: " + sharesBought);
            }

            // 前進到下一個月
            currentMonth = currentMonth.plusMonths(1);
        }

        // 6. 結算最終資產與報酬率
        // 抓取 priceList 的最後一筆作為「回測結束當天的收盤價」
        BigDecimal finalPrice = priceList.get(priceList.size() - 1).getClosePrice();

        // 最終資產 = 總股數 * 最終收盤價 (算到小數點後 2 位，即一般貨幣單位)
        BigDecimal finalAssetValue = totalShares.multiply(finalPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal returnRate = BigDecimal.ZERO;

        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            // 報酬率 = (最終資產 - 總投入本金) / 總投入本金 * 100
            BigDecimal profit = finalAssetValue.subtract(totalInvested);
            returnRate = profit.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 7. 將結果封裝進 DTO
        result.setTotalInvestedAmount(totalInvested.setScale(0, RoundingMode.DOWN));
        result.setFinalShares(totalShares.setScale(4, RoundingMode.HALF_UP));
        result.setFinalAssetValue(finalAssetValue);
        result.setTotalReturnRate(returnRate);

        return result;
    }

    /**
     * 計算定期定額 (DCA) + 股息再投入 (DRIP)
     */
    public BacktestResultDto calculateDripBacktest(String symbol, LocalDate startDate, LocalDate endDate,
                                                   BigDecimal monthlyAmount, int deductionDay) {
        BacktestResultDto result = new BacktestResultDto();
        result.setSymbol(symbol);

        Optional<StockEntity> stockOpt = stockRepository.findBySymbol(symbol);
        if (stockOpt.isEmpty()) return result;
        StockEntity stock = stockOpt.get();

        // 撈取區間內的所有價格與配息紀錄
        List<DailyPriceEntity> priceList = dailyPriceRepository.findByStockIdAndTradeDateBetweenOrderByTradeDateAsc(stock.getId(), startDate, endDate);
        List<DividendEntity> dividendList = dividendRepository.findByStockIdAndExDividendDateBetweenOrderByExDividendDateAsc(stock.getId(), startDate, endDate);

        if (priceList.isEmpty()) return result;

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalShares = BigDecimal.ZERO;

        // 紀錄「上個月是否已經扣過款」，避免假日遞延導致同個月扣兩次
        YearMonth lastDeductedMonth = null;

        // 逐日推進的迴圈
        for (DailyPriceEntity todayPrice : priceList) {
            LocalDate today = todayPrice.getTradeDate();
            YearMonth currentMonth = YearMonth.from(today);
            BigDecimal currentClosePrice = todayPrice.getClosePrice();

            if (currentClosePrice == null) continue;

            // --- 邏輯 A：檢查是否觸發「定期定額扣款」 ---
            // 決定當月的目標扣款日
            int validDay = Math.min(deductionDay, currentMonth.lengthOfMonth());
            LocalDate targetDate = currentMonth.atDay(validDay);

            // 如果今天 >= 扣款日，且這個月還沒扣過款
            if (!today.isBefore(targetDate) && !currentMonth.equals(lastDeductedMonth)) {
                BigDecimal sharesBought = monthlyAmount.divide(currentClosePrice, 4, RoundingMode.HALF_UP);
                totalInvested = totalInvested.add(monthlyAmount);
                totalShares = totalShares.add(sharesBought);
                lastDeductedMonth = currentMonth;
                System.out.println("[扣款] " + today + " | 買入價: " + currentClosePrice + " | 股數: " + sharesBought);
            }

            // --- 邏輯 B：檢查今天是否為「除息日」 ---
            for (DividendEntity dividend : dividendList) {
                if (dividend.getExDividendDate().equals(today)) {
                    // 1. 計算拿到的總股息 = 當時的總股數 * 每股現金股利
                    BigDecimal cashReceived = totalShares.multiply(dividend.getCashDividend());

                    // 2. 股息再投入：用今天的收盤價買入新股數
                    BigDecimal dripShares = cashReceived.divide(currentClosePrice, 4, RoundingMode.HALF_UP);
                    totalShares = totalShares.add(dripShares);

                    System.out.println("[除息 DRIP] " + today + " | 收到股息: " + cashReceived.setScale(0, RoundingMode.HALF_UP) + " | 買入價: " + currentClosePrice + " | 新增股數: " + dripShares);
                    break;
                }
            }
        }

        // 結算
        BigDecimal finalPrice = priceList.get(priceList.size() - 1).getClosePrice();
        BigDecimal finalAssetValue = totalShares.multiply(finalPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal returnRate = BigDecimal.ZERO;

        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = finalAssetValue.subtract(totalInvested);
            returnRate = profit.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        result.setTotalInvestedAmount(totalInvested.setScale(0, RoundingMode.DOWN));
        result.setFinalShares(totalShares.setScale(4, RoundingMode.HALF_UP));
        result.setFinalAssetValue(finalAssetValue);
        result.setTotalReturnRate(returnRate);

        return result;
    }

}
