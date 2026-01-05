package com.uex.trading.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GatewayResponseDispatcher {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void dispatch(GatewayMessage message) {
        String msgType = message.getMsgType();
        log.debug("Dispatching message type: {}", msgType);

        switch (msgType) {
            case "ORDER_RESPONSE":
                handleOrderResponse(message);
                break;
            case "CANCEL_RESPONSE":
                handleCancelResponse(message);
                break;
            case "TRADE_NOTIFY":
                handleTradeNotify(message);
                break;
            default:
                log.warn("Unknown message type: {}", msgType);
        }
    }

    private void handleOrderResponse(GatewayMessage message) {
        eventPublisher.publishEvent(new OrderResponseEvent(message));
    }

    private void handleCancelResponse(GatewayMessage message) {
        eventPublisher.publishEvent(new CancelResponseEvent(message));
    }

    private void handleTradeNotify(GatewayMessage message) {
        eventPublisher.publishEvent(new TradeNotifyEvent(message));
    }

    public static class OrderResponseEvent {
        private final GatewayMessage message;
        public OrderResponseEvent(GatewayMessage message) {
            this.message = message;
        }
        public GatewayMessage getMessage() {
            return message;
        }
    }

    public static class CancelResponseEvent {
        private final GatewayMessage message;
        public CancelResponseEvent(GatewayMessage message) {
            this.message = message;
        }
        public GatewayMessage getMessage() {
            return message;
        }
    }

    public static class TradeNotifyEvent {
        private final GatewayMessage message;
        public TradeNotifyEvent(GatewayMessage message) {
            this.message = message;
        }
        public GatewayMessage getMessage() {
            return message;
        }
    }
}
