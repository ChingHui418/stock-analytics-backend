package com.dev.stockanalyticsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class StockAnalyticsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockAnalyticsBackendApplication.class, args);
    }

}
