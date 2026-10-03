package com.dev.stockanalyticsbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping({"/", "/backtest"})
    public String index() {
        // 對應到 resources/templates/backtest.html
        return "backtest";
    }
}
