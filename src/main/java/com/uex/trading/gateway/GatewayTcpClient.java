package com.uex.trading.gateway;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GatewayTcpClient {

    @Value("${gateway.tcp.host}")
    private String host;

    @Value("${gateway.tcp.port}")
    private int port;

    @Value("${gateway.tcp.reconnect-interval}")
    private int reconnectInterval;

    @Value("${gateway.tcp.connect-timeout}")
    private int connectTimeout;

    @Autowired
    private GatewayMessageHandler messageHandler;

    private EventLoopGroup workerGroup;
    private Channel channel;
    private Bootstrap bootstrap;
    private volatile boolean shouldReconnect = true;

    @PostConstruct
    public void start() {
        workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new GatewayMessageDecoder());
                        pipeline.addLast(new GatewayMessageEncoder());
                        pipeline.addLast(messageHandler);
                        pipeline.addLast(new ReconnectHandler());
                    }
                });

        connect();
    }

    private void connect() {
        try {
            log.info("Connecting to gateway: {}:{}", host, port);
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            log.info("Connected to gateway successfully");
        } catch (Exception e) {
            log.error("Failed to connect to gateway", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (shouldReconnect) {
            log.info("Scheduling reconnect in {} ms", reconnectInterval);
            workerGroup.schedule(this::connect, reconnectInterval, TimeUnit.MILLISECONDS);
        }
    }

    public void sendMessage(GatewayMessage message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.debug("Message sent successfully: msgId={}", message.getMsgId());
                } else {
                    log.error("Failed to send message: msgId={}", message.getMsgId(), future.cause());
                }
            });
        } else {
            log.error("Cannot send message, channel is not active");
            throw new RuntimeException("Gateway connection is not available");
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down gateway client");
        shouldReconnect = false;
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    private class ReconnectHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.warn("Gateway connection lost, scheduling reconnect");
            scheduleReconnect();
            ctx.fireChannelInactive();
        }
    }
}
