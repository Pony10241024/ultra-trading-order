package com.uex.trading.controller;

import com.uex.trading.asset.AssetFlow;
import com.uex.trading.asset.AssetService;
import com.uex.trading.asset.Balance;
import com.uex.trading.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/asset")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @GetMapping("/balance")
    public ApiResponse<Balance> getBalance(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String asset) {
        try {
            Balance balance = assetService.getBalance(userId, asset);
            return ApiResponse.success(balance);
        } catch (Exception e) {
            log.error("Failed to get balance", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/balances")
    public ApiResponse<List<Balance>> getAllBalances(
            @RequestHeader("X-User-Id") String userId) {
        try {
            List<Balance> balances = assetService.getAllBalances(userId);
            return ApiResponse.success(balances);
        } catch (Exception e) {
            log.error("Failed to get all balances", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/flow")
    public ApiResponse<List<AssetFlow>> getFlowList(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String asset,
            @RequestParam(defaultValue = "100") Integer limit) {
        try {
            List<AssetFlow> flows = assetService.getFlowList(userId, asset, limit);
            return ApiResponse.success(flows);
        } catch (Exception e) {
            log.error("Failed to get flow list", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
