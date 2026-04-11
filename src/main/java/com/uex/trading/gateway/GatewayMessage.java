package com.uex.trading.gateway;

import lombok.Data;

@Data
public class GatewayMessage {
    private String msgType;     // 消息类型: *_REQUEST / *_RESPONSE
    private String msgId;       // 消息ID
    private long timestamp;     // 时间戳
    private String data;        // JSON格式的数据
}
