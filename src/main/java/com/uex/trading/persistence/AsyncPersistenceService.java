package com.uex.trading.persistence;

import com.uex.trading.asset.AssetFlow;
import com.uex.trading.asset.Balance;
import com.uex.trading.order.Order;
import com.uex.trading.order.Trade;
import com.uex.trading.repository.AssetFlowRepository;
import com.uex.trading.repository.BalanceRepository;
import com.uex.trading.repository.OrderRepository;
import com.uex.trading.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncPersistenceService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private AssetFlowRepository assetFlowRepository;

    @Async("persistenceExecutor")
    public void saveOrderAsync(Order order) {
        try {
            orderRepository.save(order);
            log.debug("Order persisted to MySQL: orderId={}", order.getOrderId());
        } catch (Exception e) {
            log.error("Failed to persist order to MySQL: orderId={}", order.getOrderId(), e);
        }
    }

    @Async("persistenceExecutor")
    public void saveTradeAsync(Trade trade) {
        try {
            tradeRepository.save(trade);
            log.debug("Trade persisted to MySQL: tradeId={}", trade.getTradeId());
        } catch (Exception e) {
            log.error("Failed to persist trade to MySQL: tradeId={}", trade.getTradeId(), e);
        }
    }

    @Async("persistenceExecutor")
    public void saveBalanceAsync(Balance balance) {
        try {
            // 查找已有记录，更新或新增
            Balance existing = balanceRepository.findByUserIdAndAsset(
                balance.getUserId(), balance.getAsset()).orElse(null);
            if (existing != null) {
                existing.setAvailable(balance.getAvailable());
                existing.setFrozen(balance.getFrozen());
                existing.setUpdateTime(balance.getUpdateTime());
                balanceRepository.save(existing);
            } else {
                balanceRepository.save(balance);
            }
            log.debug("Balance persisted to MySQL: userId={}, asset={}",
                balance.getUserId(), balance.getAsset());
        } catch (Exception e) {
            log.error("Failed to persist balance to MySQL: userId={}, asset={}",
                balance.getUserId(), balance.getAsset(), e);
        }
    }

    @Async("persistenceExecutor")
    public void saveAssetFlowAsync(AssetFlow flow) {
        try {
            assetFlowRepository.save(flow);
            log.debug("AssetFlow persisted to MySQL: flowId={}", flow.getFlowId());
        } catch (Exception e) {
            log.error("Failed to persist asset flow to MySQL: flowId={}", flow.getFlowId(), e);
        }
    }
}
