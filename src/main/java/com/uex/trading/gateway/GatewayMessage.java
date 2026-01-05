package com.uex.trading.gateway;

import lombok.Data;

@Data
public class GatewayMessage {
    private String msgType;     // 消息类型: ORDER_REQUEST, ORDER_RESPONSE, CANCEL_REQUEST, CANCEL_RESPONSE, TRADE_NOTIFY
    private String msgId;       // 消息ID
    private long timestamp;     // 时间戳
    private String data;        // JSON格式的数据
}
