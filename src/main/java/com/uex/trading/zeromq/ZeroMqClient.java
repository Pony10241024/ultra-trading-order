package com.uex.trading.zeromq;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Slf4j
@Component
public class ZeroMqClient {

    @Value("${zeromq.ems.endpoint}")
    private String emsEndpoint;

    private ZContext context;
    private ZMQ.Socket socket;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing ZeroMQ client, endpoint: {}", emsEndpoint);
            context = new ZContext();
            socket = context.createSocket(SocketType.PUSH);
            socket.connect(emsEndpoint);
            socket.setSendTimeOut(5000);
            log.info("ZeroMQ client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize ZeroMQ client", e);
            throw new RuntimeException("Failed to initialize ZeroMQ client", e);
        }
    }

    public void sendToEms(EmsMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            boolean sent = socket.send(json.getBytes(ZMQ.CHARSET), 0);

            if (sent) {
                log.info("Message sent to EMS: eventType={}, orderId={}",
                        message.getEventType(), message.getOrderId());
            } else {
                log.error("Failed to send message to EMS: {}", json);
            }
        } catch (Exception e) {
            log.error("Error sending message to EMS", e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down ZeroMQ client");
        if (socket != null) {
            socket.close();
        }
        if (context != null) {
            context.close();
        }
    }
}
