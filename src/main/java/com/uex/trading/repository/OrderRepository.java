package com.uex.trading.repository;

import com.uex.trading.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByTradeAccount(String tradeAccount);
    List<Order> findByTradeAccountAndSymbol(String tradeAccount, String symbol);
    List<Order> findByTradeAccountAndCreateTimeGreaterThanEqualAndCreateTimeLessThanOrderByCreateTimeDesc(
            String tradeAccount, Long startTime, Long endTime);
    List<Order> findByTradeAccountAndSymbolAndCreateTimeGreaterThanEqualAndCreateTimeLessThanOrderByCreateTimeDesc(
            String tradeAccount, String symbol, Long startTime, Long endTime);
}
