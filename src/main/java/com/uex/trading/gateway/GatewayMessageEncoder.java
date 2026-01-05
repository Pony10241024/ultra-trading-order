package com.uex.trading.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class GatewayMessageEncoder extends MessageToByteEncoder<GatewayMessage> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, GatewayMessage msg, ByteBuf out) throws Exception {
        String json = objectMapper.writeValueAsString(msg);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        // 协议: 4字节长度 + JSON数据
        out.writeInt(bytes.length);
        out.writeBytes(bytes);

        log.debug("Encoded message: {}", json);
    }
}
