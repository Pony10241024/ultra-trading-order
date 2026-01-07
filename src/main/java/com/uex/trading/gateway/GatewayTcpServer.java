package com.uex.trading.gateway;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GatewayTcpServer {

    @Value("${gateway.tcp.port:9900}")
    private int port;

    @Autowired
    private GatewayServerHandler serverHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new GatewayMessageDecoder());
                                pipeline.addLast(new GatewayMessageEncoder());
                                pipeline.addLast(serverHandler);
                            }
                        });

                log.info("Starting TCP server on port {}", port);
                ChannelFuture future = bootstrap.bind(port).sync();
                serverChannel = future.channel();
                log.info("TCP server started successfully on port {}", port);

                serverChannel.closeFuture().sync();
            } catch (Exception e) {
                log.error("Failed to start TCP server", e);
            } finally {
                shutdown();
            }
        }, "GatewayTcpServer").start();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TCP server");
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
