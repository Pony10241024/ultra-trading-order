package com.uex.trading.repository;

import com.uex.trading.order.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    List<Trade> findByOrderId(String orderId);
    List<Trade> findByUserId(String userId);
}
