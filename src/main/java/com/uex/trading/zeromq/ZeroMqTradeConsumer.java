package com.uex.trading.zeromq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uex.trading.order.OrderService;
import com.uex.trading.order.Trade;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Slf4j
@Component
public class ZeroMqTradeConsumer {

    @Value("${zeromq.trade.port:5556}")
    private int tradePort;

    @Autowired
    private OrderService orderService;

    private ZContext context;
    private ZMQ.Socket socket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean running = false;
    private Thread consumerThread;

    @PostConstruct
    public void start() {
        try {
            log.info("Starting ZeroMQ trade consumer on port {}", tradePort);
            context = new ZContext();
            socket = context.createSocket(SocketType.SUB);
            socket.bind("tcp://*:" + tradePort);
            socket.subscribe("".getBytes()); // Subscribe to all messages
            running = true;

            // Start consumer thread
            consumerThread = new Thread(this::consume, "ZeroMQ-TradeConsumer");
            consumerThread.start();

            log.info("ZeroMQ trade consumer started successfully");
        } catch (Exception e) {
            log.error("Failed to start ZeroMQ trade consumer", e);
            throw new RuntimeException("Failed to start ZeroMQ trade consumer", e);
        }
    }

    private void consume() {
        log.info("Trade consumer thread started");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Receive message from EMS (format: "TRADE." + Trade JSON)
                byte[] data = socket.recv(0);
                if (data == null) {
                    continue;
                }

                String messageWithTopic = new String(data, ZMQ.CHARSET);
                log.debug("Received trade notification from EMS: {}", messageWithTopic);

                // Remove "TRADE." topic prefix
                String topic = "TRADE.";
                if (!messageWithTopic.startsWith(topic)) {
                    log.warn("Received message without expected topic prefix: {}", messageWithTopic);
                    continue;
                }
                String json = messageWithTopic.substring(topic.length());

                // Parse trade object
                Trade trade = objectMapper.readValue(json, Trade.class);

                // Process trade
                orderService.handleTradeFromEms(trade);

                log.info("Processed trade from EMS: tradeId={}, orderId={}, price={}, qty={}",
                        trade.getTradeId(), trade.getOrderId(), trade.getPrice(), trade.getQuantity());

            } catch (Exception e) {
                log.error("Error processing trade notification from EMS", e);
                // Continue consuming even if one message fails
            }
        }

        log.info("Trade consumer thread stopped");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ZeroMQ trade consumer");
        running = false;

        if (consumerThread != null) {
            consumerThread.interrupt();
            try {
                consumerThread.join(5000);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for consumer thread to stop");
                Thread.currentThread().interrupt();
            }
        }

        if (socket != null) {
            socket.close();
        }
        if (context != null) {
            context.close();
        }
    }
}
