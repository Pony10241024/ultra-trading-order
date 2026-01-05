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
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody OrderRequest request) {
        try {
            Order order = orderService.submitOrder(userId, request);
            return ApiResponse.success(order);
        } catch (Exception e) {
            log.error("Failed to submit order", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{orderId}")
    public ApiResponse<String> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId) {
        try {
            orderService.cancelOrder(userId, orderId);
            return ApiResponse.success("Order cancel request submitted");
        } catch (Exception e) {
            log.error("Failed to cancel order", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<Order>> getOrderList(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String symbol) {
        try {
            List<Order> orders = orderService.getOrderList(userId, symbol);
            return ApiResponse.success(orders);
        } catch (Exception e) {
            log.error("Failed to get order list", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/trades/{orderId}")
    public ApiResponse<List<Trade>> getTradeList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId) {
        try {
            List<Trade> trades = orderService.getTradeList(userId, orderId);
            return ApiResponse.success(trades);
        } catch (Exception e) {
            log.error("Failed to get trade list", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}
