package com.uex.trading.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uex.trading.order.Order;
import com.uex.trading.order.OrderRequest;
import com.uex.trading.order.OrderService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class GatewayServerHandler extends SimpleChannelInboundHandler<GatewayMessage> {

    @Autowired
    private OrderService orderService;

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
                default:
                    log.warn("Unknown message type: {}", msg.getMsgType());
            }
        } catch (Exception e) {
            log.error("Failed to process gateway message", e);
            sendErrorResponse(ctx, msg, e.getMessage());
        }
    }

    private void handleOrderRequest(ChannelHandlerContext ctx, GatewayMessage msg) {
        try {
            // 解析OrderRequest
            OrderRequest request = objectMapper.readValue(msg.getData(), OrderRequest.class);

            // 从消息中获取userId（假设网关会传递，或者从连接上下文获取）
            // 这里暂时硬编码，实际应该从网关传来的消息中获取
            String userId = "gateway_user"; // TODO: 从网关消息中获取真实userId

            log.info("Processing order request: symbol={}, side={}, quantity={}",
                    request.getSymbol(), request.getSide(), request.getQuantity());

            // 调用OrderService处理订单
            Order order = orderService.submitOrder(userId, request);

            // 构造响应
            GatewayMessage response = new GatewayMessage();
            response.setMsgType("ORDER_RESPONSE");
            response.setMsgId(msg.getMsgId()); // 使用相同的msgId
            response.setTimestamp(System.currentTimeMillis());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("orderId", order.getOrderId());
            responseData.put("status", order.getStatus());
            responseData.put("code", 0);
            responseData.put("message", "Order submitted successfully");

            response.setData(objectMapper.writeValueAsString(responseData));

            // 发送响应
            ctx.writeAndFlush(response);
            log.info("Sent ORDER_RESPONSE: orderId={}", order.getOrderId());

        } catch (Exception e) {
            log.error("Failed to handle order request", e);
            sendErrorResponse(ctx, msg, e.getMessage());
        }
    }

    private void handleCancelRequest(ChannelHandlerContext ctx, GatewayMessage msg) {
        try {
            // 解析撤单请求
            Map<String, String> cancelData = objectMapper.readValue(msg.getData(), Map.class);
            String orderId = cancelData.get("orderId");
            String userId = cancelData.getOrDefault("userId", "gateway_user"); // TODO: 获取真实userId

            log.info("Processing cancel request: orderId={}", orderId);

            // 调用OrderService处理撤单
            orderService.cancelOrder(userId, orderId);

            // 构造响应
            GatewayMessage response = new GatewayMessage();
            response.setMsgType("CANCEL_RESPONSE");
            response.setMsgId(msg.getMsgId());
            response.setTimestamp(System.currentTimeMillis());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("orderId", orderId);
            responseData.put("status", "CANCELED");
            responseData.put("code", 0);
            responseData.put("message", "Cancel request submitted");

            response.setData(objectMapper.writeValueAsString(responseData));

            // 发送响应
            ctx.writeAndFlush(response);
            log.info("Sent CANCEL_RESPONSE: orderId={}", orderId);

        } catch (Exception e) {
            log.error("Failed to handle cancel request", e);
            sendErrorResponse(ctx, msg, e.getMessage());
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, GatewayMessage originalMsg, String errorMessage) {
        try {
            GatewayMessage response = new GatewayMessage();
            response.setMsgType(originalMsg.getMsgType().replace("REQUEST", "RESPONSE"));
            response.setMsgId(originalMsg.getMsgId());
            response.setTimestamp(System.currentTimeMillis());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("code", 9999);
            responseData.put("message", errorMessage);

            response.setData(objectMapper.writeValueAsString(responseData));
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
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
