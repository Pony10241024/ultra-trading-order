package com.uex.trading.controller;

import com.uex.trading.common.ApiResponse;
import com.uex.trading.symbol.SymbolInfo;
import com.uex.trading.symbol.SymbolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/symbol")
public class SymbolController {

    @Autowired
    private SymbolService symbolService;

    @GetMapping("/{symbol}")
    public ApiResponse<SymbolInfo> getSymbolInfo(@PathVariable String symbol) {
        try {
            SymbolInfo symbolInfo = symbolService.getSymbolInfo(symbol);
            if (symbolInfo == null) {
                return ApiResponse.error(404, "Symbol not found");
            }
            return ApiResponse.success(symbolInfo);
        } catch (Exception e) {
            log.error("Failed to get symbol info", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<SymbolInfo>> getAllSymbols() {
        try {
            List<SymbolInfo> symbols = symbolService.getAllSymbols();
            return ApiResponse.success(symbols);
        } catch (Exception e) {
            log.error("Failed to get symbol list", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
