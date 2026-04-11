package com.uex.trading.config;

import com.uex.trading.asset.AssetFlow;
import com.uex.trading.asset.Balance;
import com.uex.trading.order.Order;
import com.uex.trading.order.Trade;
import com.uex.trading.repository.AssetFlowRepository;
import com.uex.trading.repository.BalanceRepository;
import com.uex.trading.repository.OrderRepository;
import com.uex.trading.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@org.springframework.core.annotation.Order(100)
public class RedisDataInitializer implements CommandLineRunner {

    private final RedissonClient redissonClient;
    private final BalanceRepository balanceRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final AssetFlowRepository assetFlowRepository;

    @Value("${redis.keys.order-prefix}")
    private String orderPrefix;

    @Value("${redis.keys.trade-prefix}")
    private String tradePrefix;

    @Value("${redis.keys.balance-prefix}")
    private String balancePrefix;

    @Value("${redis.keys.flow-prefix}")
    private String flowPrefix;

    public RedisDataInitializer(RedissonClient redissonClient,
                                BalanceRepository balanceRepository,
                                OrderRepository orderRepository,
                                TradeRepository tradeRepository,
                                AssetFlowRepository assetFlowRepository) {
        this.redissonClient = redissonClient;
        this.balanceRepository = balanceRepository;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.assetFlowRepository = assetFlowRepository;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Starting Redis data initialization from MySQL");
            clearBusinessCache();
            loadBalances();
            loadOrders();
            loadTrades();
            loadAssetFlows();
            log.info("Redis data initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize Redis data from MySQL", e);
        }
    }

    private void clearBusinessCache() {
        deleteKeysByPattern(balancePrefix + "*");
        deleteKeysByPattern(orderPrefix + "*");
        deleteKeysByPattern(tradePrefix + "*");
        deleteKeysByPattern(flowPrefix + "*");
    }

    private void loadBalances() {
        int count = 0;
        for (Balance balance : balanceRepository.findAll()) {
            RMap<String, Object> balanceMap = redissonClient.getMap(balancePrefix + balance.getTradeAccount() + ":" + balance.getAsset());
            balanceMap.put("mainAccountId", balance.getMainAccountId());
            balanceMap.put("tradeAccount", balance.getTradeAccount());
            balanceMap.put("available", balance.getAvailable().toString());
            balanceMap.put("frozen", balance.getFrozen().toString());
            balanceMap.put("updateTime", balance.getUpdateTime().toString());
            count++;
        }
        log.info("Loaded {} balances into Redis", count);
    }

    private void loadOrders() {
        RMap<String, Order> orderMap = redissonClient.getMap(orderPrefix + "map");
        int count = 0;
        for (Order order : orderRepository.findAll()) {
            orderMap.put(order.getOrderId(), order);
            RList<String> accountOrders = redissonClient.getList(orderPrefix + "account:" + order.getTradeAccount());
            accountOrders.add(order.getOrderId());
            count++;
        }
        log.info("Loaded {} orders into Redis", count);
    }

    private void loadTrades() {
        int count = 0;
        for (Trade trade : tradeRepository.findAll()) {
            RList<Trade> orderTrades = redissonClient.getList(tradePrefix + "order:" + trade.getOrderId());
            orderTrades.add(trade);

            RList<Trade> accountTrades = redissonClient.getList(tradePrefix + "account:" + trade.getTradeAccount());
            accountTrades.add(trade);
            count++;
        }
        log.info("Loaded {} trades into Redis", count);
    }

    private void loadAssetFlows() {
        int count = 0;
        for (AssetFlow flow : assetFlowRepository.findAll()) {
            RList<AssetFlow> accountFlows = redissonClient.getList(flowPrefix + "account:" + flow.getTradeAccount());
            accountFlows.add(flow);

            RList<AssetFlow> assetFlows = redissonClient.getList(flowPrefix + "account:" + flow.getTradeAccount() + ":" + flow.getAsset());
            assetFlows.add(flow);
            count++;
        }
        log.info("Loaded {} asset flows into Redis", count);
    }

    private void deleteKeysByPattern(String pattern) {
        for (String key : redissonClient.getKeys().getKeysByPattern(pattern)) {
            redissonClient.getKeys().delete(key);
        }
    }
}
