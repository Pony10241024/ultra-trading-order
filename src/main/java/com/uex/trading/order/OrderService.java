package com.uex.trading.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uex.trading.asset.AssetService;
import com.uex.trading.common.OrderSide;
import com.uex.trading.common.OrderStatus;
import com.uex.trading.common.OrderType;
import com.uex.trading.gateway.GatewayMessage;
import com.uex.trading.gateway.GatewayResponseDispatcher;
import com.uex.trading.gateway.GatewayTcpClient;
import com.uex.trading.symbol.SymbolInfo;
import com.uex.trading.symbol.SymbolService;
import com.uex.trading.zeromq.EmsMessage;
import com.uex.trading.zeromq.ZeroMqClient;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GatewayTcpClient gatewayClient;

    @Autowired
    private ZeroMqClient zeroMqClient;

    @Autowired
    private SymbolService symbolService;

    @Autowired
    private AssetService assetService;

    @Value("${redis.keys.order-prefix}")
    private String orderPrefix;

    @Value("${redis.keys.trade-prefix}")
    private String tradePrefix;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Order submitOrder(String userId, OrderRequest request) {
        // 参数校验
        validateOrderRequest(request);

        // 创建订单
        Order order = new Order();
        order.setOrderId(generateOrderId());
        order.setUserId(userId);
        order.setSymbol(request.getSymbol());
        order.setOrderType(request.getOrderType());
        order.setSide(request.getSide());
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setFilledQty(BigDecimal.ZERO);
        order.setAvgPrice(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);
        order.setCreateTime(System.currentTimeMillis());
        order.setUpdateTime(System.currentTimeMillis());
        order.setClientOrderId(request.getClientOrderId());

        // 资金检查和冻结
        assetService.freezeAsset(userId, order);

        // 保存订单到Redis
        saveOrder(order);

        // 发送到网关
        sendOrderToGateway(order);

        // 发送消息给EMS
        notifyEmsOrderSubmit(order);

        log.info("Order submitted: orderId={}, symbol={}, side={}, type={}, price={}, qty={}",
                order.getOrderId(), order.getSymbol(), order.getSide(), order.getOrderType(),
                order.getPrice(), order.getQuantity());

        return order;
    }

    public void cancelOrder(String userId, String orderId) {
        Order order = getOrder(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }

        // 发送撤单请求到网关
        sendCancelToGateway(order);

        // 发送消息给EMS
        notifyEmsOrderCancel(order);

        log.info("Cancel order request sent: orderId={}", orderId);
    }

    public List<Order> getOrderList(String userId, String symbol) {
        RList<String> orderIds = redissonClient.getList(orderPrefix + "user:" + userId);
        List<Order> orders = new ArrayList<>();

        for (String orderId : orderIds) {
            Order order = getOrder(orderId);
            if (order != null && (symbol == null || order.getSymbol().equals(symbol))) {
                orders.add(order);
            }
        }

        return orders;
    }

    public List<Trade> getTradeList(String userId, String orderId) {
        String key = tradePrefix + "order:" + orderId;
        RList<Trade> trades = redissonClient.getList(key);
        return new ArrayList<>(trades);
    }

    @EventListener
    public void handleOrderResponse(GatewayResponseDispatcher.OrderResponseEvent event) {
        try {
            GatewayMessage message = event.getMessage();
            JsonNode data = objectMapper.readTree(message.getData());

            String orderId = data.get("orderId").asText();
            String status = data.get("status").asText();

            Order order = getOrder(orderId);
            if (order != null) {
                order.setStatus(OrderStatus.valueOf(status));
                order.setUpdateTime(System.currentTimeMillis());
                saveOrder(order);

                log.info("Order response processed: orderId={}, status={}", orderId, status);
            }
        } catch (Exception e) {
            log.error("Failed to process order response", e);
        }
    }

    @EventListener
    public void handleCancelResponse(GatewayResponseDispatcher.CancelResponseEvent event) {
        try {
            GatewayMessage message = event.getMessage();
            JsonNode data = objectMapper.readTree(message.getData());

            String orderId = data.get("orderId").asText();
            boolean success = data.get("success").asBoolean();

            if (success) {
                Order order = getOrder(orderId);
                if (order != null) {
                    order.setStatus(OrderStatus.CANCELED);
                    order.setUpdateTime(System.currentTimeMillis());
                    saveOrder(order);

                    // 解冻资金
                    assetService.unfreezeAsset(order);

                    log.info("Order canceled: orderId={}", orderId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process cancel response", e);
        }
    }

    @EventListener
    public void handleTradeNotify(GatewayResponseDispatcher.TradeNotifyEvent event) {
        try {
            GatewayMessage message = event.getMessage();
            JsonNode data = objectMapper.readTree(message.getData());

            String orderId = data.get("orderId").asText();
            String tradeId = data.get("tradeId").asText();
            BigDecimal price = new BigDecimal(data.get("price").asText());
            BigDecimal quantity = new BigDecimal(data.get("quantity").asText());
            BigDecimal fee = new BigDecimal(data.get("fee").asText());
            String feeAsset = data.get("feeAsset").asText();
            boolean isMaker = data.get("isMaker").asBoolean();

            Order order = getOrder(orderId);
            if (order == null) {
                log.error("Order not found for trade: orderId={}", orderId);
                return;
            }

            // 创建成交记录
            Trade trade = new Trade();
            trade.setTradeId(tradeId);
            trade.setOrderId(orderId);
            trade.setUserId(order.getUserId());
            trade.setSymbol(order.getSymbol());
            trade.setPrice(price);
            trade.setQuantity(quantity);
            trade.setFee(fee);
            trade.setFeeAsset(feeAsset);
            trade.setTradeTime(System.currentTimeMillis());
            trade.setMaker(isMaker);

            saveTrade(trade);

            // 更新订单
            BigDecimal filledQty = order.getFilledQty().add(quantity);
            order.setFilledQty(filledQty);

            // 更新平均成交价
            BigDecimal totalValue = order.getAvgPrice().multiply(order.getFilledQty().subtract(quantity))
                    .add(price.multiply(quantity));
            order.setAvgPrice(totalValue.divide(filledQty, 8, BigDecimal.ROUND_HALF_UP));

            // 更新订单状态
            if (filledQty.compareTo(order.getQuantity()) >= 0) {
                order.setStatus(OrderStatus.FILLED);
            } else {
                order.setStatus(OrderStatus.PARTIAL_FILLED);
            }
            order.setUpdateTime(System.currentTimeMillis());
            saveOrder(order);

            // 更新资产
            assetService.updateAssetOnTrade(trade, order);

            // 通知EMS
            notifyEmsTradeFilled(trade);

            log.info("Trade processed: tradeId={}, orderId={}, price={}, qty={}",
                    tradeId, orderId, price, quantity);
        } catch (Exception e) {
            log.error("Failed to process trade notify", e);
        }
    }

    private void validateOrderRequest(OrderRequest request) {
        SymbolInfo symbolInfo = symbolService.getSymbolInfo(request.getSymbol());
        if (symbolInfo == null) {
            throw new RuntimeException("Symbol not found: " + request.getSymbol());
        }

        if (request.getOrderType() == OrderType.LIMIT && request.getPrice() == null) {
            throw new RuntimeException("Price is required for limit order");
        }

        if (request.getQuantity().compareTo(symbolInfo.getMinOrderQty()) < 0) {
            throw new RuntimeException("Quantity below minimum: " + symbolInfo.getMinOrderQty());
        }
    }

    private void sendOrderToGateway(Order order) {
        try {
            GatewayMessage message = new GatewayMessage();
            message.setMsgType("ORDER_REQUEST");
            message.setMsgId(UUID.randomUUID().toString());
            message.setTimestamp(System.currentTimeMillis());
            message.setData(objectMapper.writeValueAsString(order));

            gatewayClient.sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send order to gateway", e);
            throw new RuntimeException("Failed to send order to gateway", e);
        }
    }

    private void sendCancelToGateway(Order order) {
        try {
            GatewayMessage message = new GatewayMessage();
            message.setMsgType("CANCEL_REQUEST");
            message.setMsgId(UUID.randomUUID().toString());
            message.setTimestamp(System.currentTimeMillis());
            message.setData("{\"orderId\":\"" + order.getOrderId() + "\"}");

            gatewayClient.sendMessage(message);
        } catch (Exception e) {
            log.error("Failed to send cancel to gateway", e);
            throw new RuntimeException("Failed to send cancel to gateway", e);
        }
    }

    private void notifyEmsOrderSubmit(Order order) {
        try {
            EmsMessage emsMessage = new EmsMessage();
            emsMessage.setEventType("ORDER_SUBMIT");
            emsMessage.setOrderId(order.getOrderId());
            emsMessage.setTimestamp(System.currentTimeMillis());
            emsMessage.setData(objectMapper.writeValueAsString(order));

            zeroMqClient.sendToEms(emsMessage);
        } catch (Exception e) {
            log.error("Failed to notify EMS for order submit", e);
        }
    }

    private void notifyEmsOrderCancel(Order order) {
        try {
            EmsMessage emsMessage = new EmsMessage();
            emsMessage.setEventType("ORDER_CANCEL");
            emsMessage.setOrderId(order.getOrderId());
            emsMessage.setTimestamp(System.currentTimeMillis());
            emsMessage.setData("{\"orderId\":\"" + order.getOrderId() + "\"}");

            zeroMqClient.sendToEms(emsMessage);
        } catch (Exception e) {
            log.error("Failed to notify EMS for order cancel", e);
        }
    }

    private void notifyEmsTradeFilled(Trade trade) {
        try {
            EmsMessage emsMessage = new EmsMessage();
            emsMessage.setEventType("TRADE_FILLED");
            emsMessage.setOrderId(trade.getOrderId());
            emsMessage.setTimestamp(System.currentTimeMillis());
            emsMessage.setData(objectMapper.writeValueAsString(trade));

            zeroMqClient.sendToEms(emsMessage);
        } catch (Exception e) {
            log.error("Failed to notify EMS for trade filled", e);
        }
    }

    private void saveOrder(Order order) {
        RMap<String, Order> orderMap = redissonClient.getMap(orderPrefix + "map");
        orderMap.put(order.getOrderId(), order);

        RList<String> userOrders = redissonClient.getList(orderPrefix + "user:" + order.getUserId());
        if (!userOrders.contains(order.getOrderId())) {
            userOrders.add(order.getOrderId());
        }
    }

    private Order getOrder(String orderId) {
        RMap<String, Order> orderMap = redissonClient.getMap(orderPrefix + "map");
        return orderMap.get(orderId);
    }

    private void saveTrade(Trade trade) {
        RList<Trade> tradeList = redissonClient.getList(tradePrefix + "order:" + trade.getOrderId());
        tradeList.add(trade);

        RList<Trade> userTrades = redissonClient.getList(tradePrefix + "user:" + trade.getUserId());
        userTrades.add(trade);
    }

    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
}
