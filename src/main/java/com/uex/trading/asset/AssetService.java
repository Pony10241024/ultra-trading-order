package com.uex.trading.asset;

import com.uex.trading.common.FlowType;
import com.uex.trading.common.OrderSide;
import com.uex.trading.order.Order;
import com.uex.trading.order.Trade;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AssetService {

    @Autowired
    private RedissonClient redissonClient;

    @Value("${redis.keys.balance-prefix}")
    private String balancePrefix;

    @Value("${redis.keys.flow-prefix}")
    private String flowPrefix;

    public Balance getBalance(String userId, String asset) {
        String key = balancePrefix + userId + ":" + asset;
        RMap<String, Object> balanceMap = redissonClient.getMap(key);

        if (balanceMap.isEmpty()) {
            Balance balance = new Balance();
            balance.setUserId(userId);
            balance.setAsset(asset);
            balance.setAvailable(BigDecimal.ZERO);
            balance.setFrozen(BigDecimal.ZERO);
            balance.setUpdateTime(System.currentTimeMillis());
            return balance;
        }

        Balance balance = new Balance();
        balance.setUserId(userId);
        balance.setAsset(asset);
        balance.setAvailable(new BigDecimal(balanceMap.get("available").toString()));
        balance.setFrozen(new BigDecimal(balanceMap.get("frozen").toString()));
        balance.setUpdateTime(Long.parseLong(balanceMap.get("updateTime").toString()));

        return balance;
    }

    public List<Balance> getAllBalances(String userId) {
        String pattern = balancePrefix + userId + ":*";
        List<Balance> balances = new ArrayList<>();

        for (String key : redissonClient.getKeys().getKeysByPattern(pattern)) {
            String asset = key.substring(key.lastIndexOf(":") + 1);
            Balance balance = getBalance(userId, asset);
            if (balance.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                balances.add(balance);
            }
        }

        return balances;
    }

    public void freezeAsset(String userId, Order order) {
        String asset;
        BigDecimal amount;

        if (order.getSide() == OrderSide.BUY) {
            // 买单冻结计价货币
            asset = order.getSymbol().replaceAll("^[A-Z]+", "");
            if (order.getPrice() != null) {
                amount = order.getPrice().multiply(order.getQuantity());
            } else {
                throw new RuntimeException("Market buy order not supported yet");
            }
        } else {
            // 卖单冻结基础货币
            asset = order.getSymbol().replaceAll("[A-Z]+$", "");
            amount = order.getQuantity();
        }

        Balance balance = getBalance(userId, asset);
        if (balance.getAvailable().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance: " + asset);
        }

        balance.setAvailable(balance.getAvailable().subtract(amount));
        balance.setFrozen(balance.getFrozen().add(amount));
        balance.setUpdateTime(System.currentTimeMillis());

        saveBalance(balance);

        log.info("Asset frozen: userId={}, asset={}, amount={}", userId, asset, amount);
    }

    public void unfreezeAsset(Order order) {
        String asset;
        BigDecimal amount;

        if (order.getSide() == OrderSide.BUY) {
            asset = order.getSymbol().replaceAll("^[A-Z]+", "");
            BigDecimal remainQty = order.getQuantity().subtract(order.getFilledQty());
            amount = order.getPrice().multiply(remainQty);
        } else {
            asset = order.getSymbol().replaceAll("[A-Z]+$", "");
            amount = order.getQuantity().subtract(order.getFilledQty());
        }

        Balance balance = getBalance(order.getUserId(), asset);
        balance.setFrozen(balance.getFrozen().subtract(amount));
        balance.setAvailable(balance.getAvailable().add(amount));
        balance.setUpdateTime(System.currentTimeMillis());

        saveBalance(balance);

        log.info("Asset unfrozen: userId={}, asset={}, amount={}", order.getUserId(), asset, amount);
    }

    public void updateAssetOnTrade(Trade trade, Order order) {
        String userId = order.getUserId();

        if (order.getSide() == OrderSide.BUY) {
            // 买入：扣除冻结的计价货币，增加基础货币
            String quoteAsset = order.getSymbol().replaceAll("^[A-Z]+", "");
            String baseAsset = order.getSymbol().replaceAll("[A-Z]+$", "");

            // 扣除冻结的计价货币
            BigDecimal cost = trade.getPrice().multiply(trade.getQuantity());
            Balance quoteBalance = getBalance(userId, quoteAsset);
            quoteBalance.setFrozen(quoteBalance.getFrozen().subtract(cost));
            quoteBalance.setUpdateTime(System.currentTimeMillis());
            saveBalance(quoteBalance);

            // 增加基础货币（扣除手续费）
            BigDecimal receivedQty = trade.getQuantity().subtract(trade.getFee());
            Balance baseBalance = getBalance(userId, baseAsset);
            baseBalance.setAvailable(baseBalance.getAvailable().add(receivedQty));
            baseBalance.setUpdateTime(System.currentTimeMillis());
            saveBalance(baseBalance);

            // 记录流水
            recordFlow(userId, quoteAsset, FlowType.TRADE_OUT, cost.negate(),
                    quoteBalance.getAvailable(), trade.getTradeId(), "Buy " + order.getSymbol());
            recordFlow(userId, baseAsset, FlowType.TRADE_IN, receivedQty,
                    baseBalance.getAvailable(), trade.getTradeId(), "Buy " + order.getSymbol());
            recordFlow(userId, baseAsset, FlowType.FEE, trade.getFee().negate(),
                    baseBalance.getAvailable(), trade.getTradeId(), "Trade fee");

        } else {
            // 卖出：扣除冻结的基础货币，增加计价货币
            String baseAsset = order.getSymbol().replaceAll("[A-Z]+$", "");
            String quoteAsset = order.getSymbol().replaceAll("^[A-Z]+", "");

            // 扣除冻结的基础货币
            Balance baseBalance = getBalance(userId, baseAsset);
            baseBalance.setFrozen(baseBalance.getFrozen().subtract(trade.getQuantity()));
            baseBalance.setUpdateTime(System.currentTimeMillis());
            saveBalance(baseBalance);

            // 增加计价货币（扣除手续费）
            BigDecimal receivedAmount = trade.getPrice().multiply(trade.getQuantity()).subtract(trade.getFee());
            Balance quoteBalance = getBalance(userId, quoteAsset);
            quoteBalance.setAvailable(quoteBalance.getAvailable().add(receivedAmount));
            quoteBalance.setUpdateTime(System.currentTimeMillis());
            saveBalance(quoteBalance);

            // 记录流水
            recordFlow(userId, baseAsset, FlowType.TRADE_OUT, trade.getQuantity().negate(),
                    baseBalance.getAvailable(), trade.getTradeId(), "Sell " + order.getSymbol());
            recordFlow(userId, quoteAsset, FlowType.TRADE_IN, receivedAmount,
                    quoteBalance.getAvailable(), trade.getTradeId(), "Sell " + order.getSymbol());
            recordFlow(userId, quoteAsset, FlowType.FEE, trade.getFee().negate(),
                    quoteBalance.getAvailable(), trade.getTradeId(), "Trade fee");
        }

        log.info("Asset updated on trade: userId={}, tradeId={}", userId, trade.getTradeId());
    }

    public List<AssetFlow> getFlowList(String userId, String asset, Integer limit) {
        String key = flowPrefix + "user:" + userId;
        if (asset != null) {
            key += ":" + asset;
        }

        RList<AssetFlow> flowList = redissonClient.getList(key);
        List<AssetFlow> flows = new ArrayList<>(flowList);

        if (limit != null && flows.size() > limit) {
            return flows.subList(flows.size() - limit, flows.size());
        }

        return flows;
    }

    private void saveBalance(Balance balance) {
        String key = balancePrefix + balance.getUserId() + ":" + balance.getAsset();
        RMap<String, Object> balanceMap = redissonClient.getMap(key);

        balanceMap.put("available", balance.getAvailable().toString());
        balanceMap.put("frozen", balance.getFrozen().toString());
        balanceMap.put("updateTime", balance.getUpdateTime().toString());
    }

    private void recordFlow(String userId, String asset, FlowType flowType, BigDecimal amount,
                           BigDecimal balance, String relatedId, String description) {
        AssetFlow flow = new AssetFlow();
        flow.setFlowId(generateFlowId());
        flow.setUserId(userId);
        flow.setAsset(asset);
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setBalance(balance);
        flow.setRelatedId(relatedId);
        flow.setDescription(description);
        flow.setCreateTime(System.currentTimeMillis());

        String key = flowPrefix + "user:" + userId;
        RList<AssetFlow> flowList = redissonClient.getList(key);
        flowList.add(flow);

        String assetKey = flowPrefix + "user:" + userId + ":" + asset;
        RList<AssetFlow> assetFlowList = redissonClient.getList(assetKey);
        assetFlowList.add(flow);

        log.debug("Flow recorded: userId={}, asset={}, type={}, amount={}",
                userId, asset, flowType, amount);
    }

    private String generateFlowId() {
        return "FLOW" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
