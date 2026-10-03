package com.dev.stockanalyticsbackend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "dividends")
public class DividendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多對一
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Column(name = "announce_date")
    private LocalDate announceDate;

    @Column(name = "ex_dividend_date")
    private LocalDate exDividendDate;

    @Column(name = "cash_dividend", precision = 10, scale = 4)
    private BigDecimal cashDividend;

    @Column(name = "stock_dividend", precision = 10, scale = 4)
    private BigDecimal stockDividend;

    public DividendEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockEntity getStock() {
        return stock;
    }

    public void setStock(StockEntity stock) {
        this.stock = stock;
    }

    public LocalDate getAnnounceDate() {
        return announceDate;
    }

    public void setAnnounceDate(LocalDate announceDate) {
        this.announceDate = announceDate;
    }

    public LocalDate getExDividendDate() {
        return exDividendDate;
    }

    public void setExDividendDate(LocalDate exDividendDate) {
        this.exDividendDate = exDividendDate;
    }

    public BigDecimal getCashDividend() {
        return cashDividend;
    }

    public void setCashDividend(BigDecimal cashDividend) {
        this.cashDividend = cashDividend;
    }

    public BigDecimal getStockDividend() {
        return stockDividend;
    }

    public void setStockDividend(BigDecimal stockDividend) {
        this.stockDividend = stockDividend;
    }
}
