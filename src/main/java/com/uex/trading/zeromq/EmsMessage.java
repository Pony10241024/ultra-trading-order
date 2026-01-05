package com.uex.trading.zeromq;

import lombok.Data;

@Data
public class EmsMessage {
    private String eventType;   // ORDER_SUBMIT, ORDER_CANCEL, TRADE_FILLED
    private String orderId;
    private long timestamp;
    private String data;        // JSON格式的详细数据
}
