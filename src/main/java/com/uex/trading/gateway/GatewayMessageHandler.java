package com.uex.trading.gateway;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GatewayMessageHandler extends SimpleChannelInboundHandler<GatewayMessage> {

    @Autowired
    private GatewayResponseDispatcher responseDispatcher;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GatewayMessage msg) {
        log.info("Received gateway message: type={}, msgId={}", msg.getMsgType(), msg.getMsgId());

        try {
            responseDispatcher.dispatch(msg);
        } catch (Exception e) {
            log.error("Failed to process gateway message", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Gateway connection established: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("Gateway connection closed: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Gateway handler exception", cause);
        ctx.close();
    }
}
