package com.dev.stockanalyticsbackend.dto;

import java.math.BigDecimal;

public class BacktestResultDto {

    // 投資標的代號
    private String symbol;
    // 總投入本金
    private BigDecimal totalInvestedAmount;
    // 最終累積的總股數
    private BigDecimal finalShares;
    // 最終總資產價值
    private BigDecimal finalAssetValue;
    // 總報酬率
    private BigDecimal totalReturnRate;

    public BacktestResultDto() {
        // 無參數建構子中將數值預設為 BigDecimal.ZERO，避免前端在渲染圖表時遇到 NullPointerException
        this.totalInvestedAmount = BigDecimal.ZERO;
        this.finalShares = BigDecimal.ZERO;
        this.finalAssetValue = BigDecimal.ZERO;
        this.totalReturnRate = BigDecimal.ZERO;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getTotalInvestedAmount() {
        return totalInvestedAmount;
    }

    public void setTotalInvestedAmount(BigDecimal totalInvestedAmount) {
        this.totalInvestedAmount = totalInvestedAmount;
    }

    public BigDecimal getFinalShares() {
        return finalShares;
    }

    public void setFinalShares(BigDecimal finalShares) {
        this.finalShares = finalShares;
    }

    public BigDecimal getFinalAssetValue() {
        return finalAssetValue;
    }

    public void setFinalAssetValue(BigDecimal finalAssetValue) {
        this.finalAssetValue = finalAssetValue;
    }

    public BigDecimal getTotalReturnRate() {
        return totalReturnRate;
    }

    public void setTotalReturnRate(BigDecimal totalReturnRate) {
        this.totalReturnRate = totalReturnRate;
    }
}
