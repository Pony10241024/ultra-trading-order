package com.uex.trading.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uex.trading.asset.AssetFlow;
import com.uex.trading.asset.AssetService;
import com.uex.trading.asset.Balance;
import com.uex.trading.common.ApiResponse;
import com.uex.trading.order.Order;
import com.uex.trading.order.OrderRequest;
import com.uex.trading.order.OrderService;
import com.uex.trading.order.Trade;
import com.uex.trading.symbol.SymbolInfo;
import com.uex.trading.symbol.SymbolService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ChannelHandler.Sharable
public class GatewayServerHandler extends SimpleChannelInboundHandler<GatewayMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private SymbolService symbolService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GatewayMessage msg) {
        log.info("Received from gateway: type={}, msgId={}", msg.getMsgType(), msg.getMsgId());

        try {
            switch (msg.getMsgType()) {
                case "ORDER_REQUEST":
                    handleOrderRequest(ctx, msg);
                    break;
                case "CANCEL_REQUEST":
                    handleCancelRequest(ctx, msg);
                    break;
                case "ORDER_LIST_REQUEST":
                    handleOrderListRequest(ctx, msg);
                    break;
                case "ORDER_TODAY_REQUEST":
                    handleTodayOrderListRequest(ctx, msg);
                    break;
                case "TRADE_LIST_REQUEST":
                    handleTradeListRequest(ctx, msg);
                    break;
                case "SYMBOL_INFO_REQUEST":
                    handleSymbolInfoRequest(ctx, msg);
                    break;
                case "SYMBOL_LIST_REQUEST":
                    handleSymbolListRequest(ctx, msg);
                    break;
                case "ASSET_INCREASE_REQUEST":
                    handleAssetIncreaseRequest(ctx, msg);
                    break;
                case "ASSET_BALANCE_REQUEST":
                    handleAssetBalanceRequest(ctx, msg);
                    break;
                case "ASSET_BALANCES_REQUEST":
                    handleAssetBalancesRequest(ctx, msg);
                    break;
                case "ASSET_FLOW_REQUEST":
                    handleAssetFlowRequest(ctx, msg);
                    break;
                default:
                    log.warn("Unknown message type: {}", msg.getMsgType());
                    sendErrorResponse(ctx, msg, "Unsupported message type: " + msg.getMsgType());
            }
        } catch (Exception e) {
            log.error("Failed to process gateway message", e);
            sendErrorResponse(ctx, msg, e.getMessage());
        }
    }

    private void handleOrderRequest(ChannelHandlerContext ctx, GatewayMessage msg) {
        try {
            ObjectNode requestNode = readData(msg);
            String mainAccountId = resolveMainAccountId(requestNode);
            String tradeAccount = resolveTradeAccount(requestNode);
            requestNode.remove("userId");
            requestNode.remove("mainAccountId");
            requestNode.remove("tradeAccount");
            OrderRequest request = objectMapper.treeToValue(requestNode, OrderRequest.class);

            log.info("Processing order request: symbol={}, side={}, quantity={}",
                    request.getSymbol(), request.getSide(), request.getQuantity());

            Order order = orderService.submitOrder(mainAccountId, tradeAccount, request);
            sendSuccessResponse(ctx, msg, order);
            log.info("Sent ORDER_RESPONSE: orderId={}", order.getOrderId());

        } catch (Exception e) {
            log.error("Failed to handle order request", e);
            sendErrorResponse(ctx, msg, e.getMessage());
        }
    }

    private void handleCancelRequest(ChannelHandlerContext ctx, GatewayMessage msg) {
        try {
            ObjectNode requestNode = readData(msg);
            String orderId = getRequiredText(requestNode, "orderId");
            String tradeAccount = resolveTradeAccount(requestNode);

            log.info("Processing cancel request: orderId={}", orderId);

            orderService.cancelOrder(tradeAccount, orderId);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("orderId", orderId);
            responseData.put("status", "CANCELED");
            responseData.put("message", "Cancel request submitted");
            sendSuccessResponse(ctx, msg, responseData);
            log.info("Sent CANCEL_RESPONSE: orderId={}", orderId);

        } catch (Exception e) {
            log.error("Failed to handle cancel request", e);
            sendErrorResponse(ctx, msg, e.getMessage());
        }
    }

    private void handleOrderListRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String tradeAccount = resolveTradeAccount(requestNode);
        String symbol = getOptionalText(requestNode, "symbol");
        List<Order> orders = orderService.getOrderList(tradeAccount, symbol);
        sendSuccessResponse(ctx, msg, orders);
    }

    private void handleTodayOrderListRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String tradeAccount = resolveTradeAccount(requestNode);
        String symbol = getOptionalText(requestNode, "symbol");
        List<Order> orders = orderService.getTodayOrderList(tradeAccount, symbol);
        sendSuccessResponse(ctx, msg, orders);
    }

    private void handleTradeListRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String tradeAccount = resolveTradeAccount(requestNode);
        String orderId = getRequiredText(requestNode, "orderId");
        List<Trade> trades = orderService.getTradeList(tradeAccount, orderId);
        sendSuccessResponse(ctx, msg, trades);
    }

    private void handleSymbolInfoRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String symbol = getRequiredText(requestNode, "symbol");
        SymbolInfo symbolInfo = symbolService.getSymbolInfo(symbol);
        if (symbolInfo == null) {
            sendResponse(ctx, msg, ApiResponse.error(404, "Symbol not found"));
            return;
        }
        sendSuccessResponse(ctx, msg, symbolInfo);
    }

    private void handleSymbolListRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        sendSuccessResponse(ctx, msg, symbolService.getAllSymbols());
    }

    private void handleAssetIncreaseRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String mainAccountId = resolveMainAccountId(requestNode);
        String tradeAccount = resolveTradeAccount(requestNode);
        String asset = getRequiredText(requestNode, "asset");
        BigDecimal amount = getRequiredDecimal(requestNode, "amount");
        String description = getOptionalText(requestNode, "description");
        Balance balance = assetService.increaseAsset(mainAccountId, tradeAccount, asset, amount, description);
        sendSuccessResponse(ctx, msg, balance);
    }

    private void handleAssetBalanceRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String mainAccountId = resolveMainAccountId(requestNode);
        String tradeAccount = resolveTradeAccount(requestNode);
        String asset = getRequiredText(requestNode, "asset");
        Balance balance = assetService.getBalance(mainAccountId, tradeAccount, asset);
        sendSuccessResponse(ctx, msg, balance);
    }

    private void handleAssetBalancesRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String mainAccountId = resolveMainAccountId(requestNode);
        String tradeAccount = resolveTradeAccount(requestNode);
        List<Balance> balances = assetService.getAllBalances(mainAccountId, tradeAccount);
        sendSuccessResponse(ctx, msg, balances);
    }

    private void handleAssetFlowRequest(ChannelHandlerContext ctx, GatewayMessage msg) throws Exception {
        ObjectNode requestNode = readData(msg);
        String tradeAccount = resolveTradeAccount(requestNode);
        String asset = getOptionalText(requestNode, "asset");
        Integer limit = requestNode.has("limit") ? requestNode.get("limit").asInt(100) : 100;
        List<AssetFlow> flows = assetService.getFlowList(tradeAccount, asset, limit);
        sendSuccessResponse(ctx, msg, flows);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, GatewayMessage originalMsg, String errorMessage) {
        try {
            sendResponse(ctx, originalMsg, ApiResponse.error(9999, errorMessage));
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx, GatewayMessage request, Object data) throws Exception {
        sendResponse(ctx, request, ApiResponse.success(data));
    }

    private void sendResponse(ChannelHandlerContext ctx, GatewayMessage request, ApiResponse<?> body) throws Exception {
        GatewayMessage response = new GatewayMessage();
        response.setMsgType(resolveResponseType(request.getMsgType()));
        response.setMsgId(request.getMsgId());
        response.setTimestamp(System.currentTimeMillis());
        response.setData(objectMapper.writeValueAsString(body));
        ctx.writeAndFlush(response);
    }

    private String resolveResponseType(String requestType) {
        return requestType.endsWith("_REQUEST")
                ? requestType.replace("_REQUEST", "_RESPONSE")
                : requestType + "_RESPONSE";
    }

    private ObjectNode readData(GatewayMessage msg) throws Exception {
        if (msg.getData() == null || msg.getData().isBlank()) {
            return objectMapper.createObjectNode();
        }
        return (ObjectNode) objectMapper.readTree(msg.getData());
    }

    private String getRequiredText(ObjectNode node, String field) {
        String value = getOptionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    private String getText(ObjectNode node, String field, String defaultValue) {
        String value = getOptionalText(node, field);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String getOptionalText(ObjectNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private BigDecimal getRequiredDecimal(ObjectNode node, String field) {
        String value = getRequiredText(node, field);
        return new BigDecimal(value);
    }

    private String resolveMainAccountId(ObjectNode node) {
        String mainAccountId = getOptionalText(node, "mainAccountId");
        if (mainAccountId != null && !mainAccountId.isBlank()) {
            return mainAccountId;
        }
        String userId = getOptionalText(node, "userId");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        throw new IllegalArgumentException("Missing required field: mainAccountId");
    }

    private String resolveTradeAccount(ObjectNode node) {
        String tradeAccount = getOptionalText(node, "tradeAccount");
        if (tradeAccount != null && !tradeAccount.isBlank()) {
            return tradeAccount;
        }
        String userId = getOptionalText(node, "userId");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        throw new IllegalArgumentException("Missing required field: tradeAccount");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Gateway connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("Gateway disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Handler exception", cause);
        ctx.close();
    }
}
