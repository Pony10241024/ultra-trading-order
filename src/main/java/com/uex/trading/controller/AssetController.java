package com.uex.trading.controller;

import com.uex.trading.asset.AssetAdjustRequest;
import com.uex.trading.asset.AssetFlow;
import com.uex.trading.asset.AssetService;
import com.uex.trading.asset.Balance;
import com.uex.trading.common.ApiResponse;
import jakarta.validation.Valid;
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
            @RequestHeader(value = "X-Main-Account-Id", required = false) String mainAccountId,
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam String asset) {
        try {
            Balance balance = assetService.getBalance(
                    resolveMainAccountId(mainAccountId, userId),
                    resolveTradeAccount(tradeAccount, userId),
                    asset);
            return ApiResponse.success(balance);
        } catch (Exception e) {
            log.error("Failed to get balance", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/increase")
    public ApiResponse<Balance> increaseAsset(
            @RequestHeader(value = "X-Main-Account-Id", required = false) String mainAccountId,
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody AssetAdjustRequest request) {
        try {
            Balance balance = assetService.increaseAsset(
                    resolveMainAccountId(mainAccountId, userId),
                    resolveTradeAccount(tradeAccount, userId),
                    request.getAsset(),
                    request.getAmount(),
                    request.getDescription());
            return ApiResponse.success(balance);
        } catch (Exception e) {
            log.error("Failed to increase asset", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/balances")
    public ApiResponse<List<Balance>> getAllBalances(
            @RequestHeader(value = "X-Main-Account-Id", required = false) String mainAccountId,
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            List<Balance> balances = assetService.getAllBalances(
                    resolveMainAccountId(mainAccountId, userId),
                    resolveTradeAccount(tradeAccount, userId));
            return ApiResponse.success(balances);
        } catch (Exception e) {
            log.error("Failed to get all balances", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/flow")
    public ApiResponse<List<AssetFlow>> getFlowList(
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String asset,
            @RequestParam(defaultValue = "100") Integer limit) {
        try {
            List<AssetFlow> flows = assetService.getFlowList(resolveTradeAccount(tradeAccount, userId), asset, limit);
            return ApiResponse.success(flows);
        } catch (Exception e) {
            log.error("Failed to get flow list", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    private String resolveMainAccountId(String mainAccountId, String userId) {
        if (mainAccountId != null && !mainAccountId.isBlank()) {
            return mainAccountId;
        }
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        throw new RuntimeException("Missing request header: X-Main-Account-Id");
    }

    private String resolveTradeAccount(String tradeAccount, String userId) {
        if (tradeAccount != null && !tradeAccount.isBlank()) {
            return tradeAccount;
        }
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        throw new RuntimeException("Missing request header: X-Trade-Account");
    }
}
