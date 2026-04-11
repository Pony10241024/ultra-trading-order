package com.uex.trading.controller;

import com.uex.trading.common.ApiResponse;
import com.uex.trading.order.Order;
import com.uex.trading.order.OrderRequest;
import com.uex.trading.order.OrderService;
import com.uex.trading.order.Trade;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    public ApiResponse<Order> submitOrder(
            @RequestHeader(value = "X-Main-Account-Id", required = false) String mainAccountId,
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody OrderRequest request) {
        try {
            Order order = orderService.submitOrder(
                    resolveMainAccountId(mainAccountId, userId),
                    resolveTradeAccount(tradeAccount, userId),
                    request);
            return ApiResponse.success(order);
        } catch (Exception e) {
            log.error("Failed to submit order", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{orderId}")
    public ApiResponse<String> cancelOrder(
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String orderId) {
        try {
            orderService.cancelOrder(resolveTradeAccount(tradeAccount, userId), orderId);
            return ApiResponse.success("Order cancel request submitted");
        } catch (Exception e) {
            log.error("Failed to cancel order", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<Order>> getOrderList(
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String symbol) {
        try {
            List<Order> orders = orderService.getOrderList(resolveTradeAccount(tradeAccount, userId), symbol);
            return ApiResponse.success(orders);
        } catch (Exception e) {
            log.error("Failed to get order list", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/today")
    public ApiResponse<List<Order>> getTodayOrderList(
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(required = false) String symbol) {
        try {
            List<Order> orders = orderService.getTodayOrderList(resolveTradeAccount(tradeAccount, userId), symbol);
            return ApiResponse.success(orders);
        } catch (Exception e) {
            log.error("Failed to get today order list", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/trades/{orderId}")
    public ApiResponse<List<Trade>> getTradeList(
            @RequestHeader(value = "X-Trade-Account", required = false) String tradeAccount,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String orderId) {
        try {
            List<Trade> trades = orderService.getTradeList(resolveTradeAccount(tradeAccount, userId), orderId);
            return ApiResponse.success(trades);
        } catch (Exception e) {
            log.error("Failed to get trade list", e);
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
