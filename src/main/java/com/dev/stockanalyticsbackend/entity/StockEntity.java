package com.dev.stockanalyticsbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stocks")
public class StockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", length = 10, nullable = false, unique = true)
    private String symbol;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "asset_type", length = 20, nullable = false)
    private String assetType;

    public StockEntity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }
}
