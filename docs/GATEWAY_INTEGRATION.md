# C++网关对接文档

## 1. 概述

本文档描述Java订单服务与C++交易网关的TCP通信协议和对接规范。

### 1.1 通信架构

```
┌─────────────────┐         TCP/IP          ┌─────────────────┐
│                 │ ◄─────────────────────► │                 │
│  订单服务(Java) │    长连接 + 心跳         │  交易网关(C++)  │
│                 │                          │                 │
└─────────────────┘                          └─────────────────┘
```

### 1.2 连接信息

- **协议**: TCP
- **默认地址**: 127.0.0.1:9900
- **连接模式**: 长连接
- **重连机制**: 自动重连，间隔5秒
- **超时设置**: 连接超时10秒

## 2. 通信协议

### 2.1 协议格式

每个消息包含**消息头**和**消息体**：

```
┌──────────────┬──────────────────────────┐
│  消息头(4B)  │      消息体(变长)         │
├──────────────┼──────────────────────────┤
│  Body Length │      JSON String         │
└──────────────┴──────────────────────────┘
```

- **消息头**: 4字节整数（Big Endian），表示消息体长度
- **消息体**: UTF-8编码的JSON字符串

### 2.2 消息体JSON格式

```json
{
  "msgType": "消息类型",
  "msgId": "消息唯一ID(UUID)",
  "timestamp": 1234567890123,
  "data": "业务数据JSON字符串"
}
```

字段说明：
- **msgType**: 消息类型标识，见下文
- **msgId**: 消息唯一标识符，用于追踪和日志
- **timestamp**: 消息时间戳（毫秒）
- **data**: 业务数据，序列化为JSON字符串

## 3. 消息类型定义

### 3.1 下单请求 (ORDER_REQUEST)

**方向**: 订单服务 → 交易网关

**msgType**: `ORDER_REQUEST`

**data字段内容**:
```json
{
  "orderId": "ORD1704518400001abc123",
  "userId": "user123",
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": "50000.00",
  "quantity": "0.1",
  "clientOrderId": "client_order_001"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | String | 是 | 系统订单ID |
| userId | String | 是 | 用户ID |
| symbol | String | 是 | 交易对 |
| orderType | String | 是 | 订单类型：LIMIT(限价)/MARKET(市价) |
| side | String | 是 | 买卖方向：BUY/SELL |
| price | String | 否 | 价格（市价单可为空） |
| quantity | String | 是 | 数量 |
| clientOrderId | String | 否 | 客户端订单ID |

**完整示例**:
```json
{
  "msgType": "ORDER_REQUEST",
  "msgId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1704518400123,
  "data": "{\"orderId\":\"ORD1704518400001abc123\",\"userId\":\"user123\",\"symbol\":\"BTCUSDT\",\"orderType\":\"LIMIT\",\"side\":\"BUY\",\"price\":\"50000.00\",\"quantity\":\"0.1\"}"
}
```

### 3.2 下单响应 (ORDER_RESPONSE)

**方向**: 交易网关 → 订单服务

**msgType**: `ORDER_RESPONSE`

**data字段内容**:
```json
{
  "orderId": "ORD1704518400001abc123",
  "status": "SUBMITTED",
  "gatewayOrderId": "GW_20240106_001",
  "message": "Order submitted successfully"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | String | 是 | 系统订单ID |
| status | String | 是 | 订单状态：SUBMITTED/REJECTED |
| gatewayOrderId | String | 否 | 网关订单ID |
| message | String | 否 | 响应消息 |

**状态枚举**:
- `SUBMITTED`: 已提交到交易所
- `REJECTED`: 被拒绝
- `PARTIAL_FILLED`: 部分成交
- `FILLED`: 完全成交

### 3.3 撤单请求 (CANCEL_REQUEST)

**方向**: 订单服务 → 交易网关

**msgType**: `CANCEL_REQUEST`

**data字段内容**:
```json
{
  "orderId": "ORD1704518400001abc123"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | String | 是 | 系统订单ID |

### 3.4 撤单响应 (CANCEL_RESPONSE)

**方向**: 交易网关 → 订单服务

**msgType**: `CANCEL_RESPONSE`

**data字段内容**:
```json
{
  "orderId": "ORD1704518400001abc123",
  "success": true,
  "message": "Order canceled successfully"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | String | 是 | 系统订单ID |
| success | Boolean | 是 | 是否成功 |
| message | String | 否 | 响应消息 |

### 3.5 成交通知 (TRADE_NOTIFY)

**方向**: 交易网关 → 订单服务

**msgType**: `TRADE_NOTIFY`

**data字段内容**:
```json
{
  "orderId": "ORD1704518400001abc123",
  "tradeId": "TRD1704518400001xyz789",
  "price": "50000.00",
  "quantity": "0.05",
  "fee": "0.00005",
  "feeAsset": "BTC",
  "isMaker": true,
  "tradeTime": 1704518400500
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | String | 是 | 系统订单ID |
| tradeId | String | 是 | 成交ID |
| price | String | 是 | 成交价格 |
| quantity | String | 是 | 成交数量 |
| fee | String | 是 | 手续费 |
| feeAsset | String | 是 | 手续费币种 |
| isMaker | Boolean | 是 | 是否为Maker |
| tradeTime | Long | 否 | 成交时间戳 |

### 3.6 心跳消息 (HEARTBEAT)

**方向**: 双向

**msgType**: `HEARTBEAT`

**data字段内容**:
```json
{
  "ping": "pong"
}
```

**说明**:
- 每30秒发送一次心跳
- 用于保持连接活跃
- 接收方需响应相同的心跳消息

## 4. 编码实现参考

### 4.1 消息编码器（Java端）

```java
public class GatewayMessageEncoder extends MessageToByteEncoder<GatewayMessage> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, GatewayMessage msg, ByteBuf out) throws Exception {
        // 序列化消息为JSON
        String json = objectMapper.writeValueAsString(msg);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        // 写入长度（4字节，Big Endian）
        out.writeInt(bytes.length);
        // 写入消息体
        out.writeBytes(bytes);
    }
}
```

### 4.2 消息解码器（Java端）

```java
public class GatewayMessageDecoder extends ByteToMessageDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的字节读取长度
        if (in.readableBytes() < 4) {
            return;
        }

        // 标记当前位置
        in.markReaderIndex();

        // 读取消息长度
        int length = in.readInt();

        // 检查消息体是否完整
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        // 读取消息体
        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        // 解析JSON
        String json = new String(bytes, StandardCharsets.UTF_8);
        GatewayMessage message = objectMapper.readValue(json, GatewayMessage.class);

        out.add(message);
    }
}
```

### 4.3 C++端伪代码参考

```cpp
// 消息发送
void sendMessage(const GatewayMessage& msg) {
    // 序列化为JSON
    std::string json = serializeToJson(msg);

    // 消息长度（Big Endian）
    uint32_t length = htonl(json.length());

    // 发送长度
    send(socket, &length, 4, 0);

    // 发送消息体
    send(socket, json.c_str(), json.length(), 0);
}

// 消息接收
GatewayMessage receiveMessage() {
    // 读取长度
    uint32_t length;
    recv(socket, &length, 4, 0);
    length = ntohl(length);

    // 读取消息体
    std::vector<char> buffer(length);
    recv(socket, buffer.data(), length, 0);

    // 解析JSON
    std::string json(buffer.begin(), buffer.end());
    return parseFromJson(json);
}
```

## 5. 异常处理

### 5.1 连接异常

**场景**: 网关服务不可用或网络中断

**处理**:
- 自动重连，间隔5秒
- 记录错误日志
- 通知监控系统

### 5.2 消息异常

**场景**: 接收到格式错误的消息

**处理**:
- 记录错误日志（包含原始消息）
- 丢弃该消息
- 继续处理后续消息

### 5.3 超时处理

**场景**: 发送消息后长时间未收到响应

**处理**:
- 下单/撤单超时时间：10秒
- 超时后标记订单状态为`TIMEOUT`
- 可选：发送查询请求确认订单状态

## 6. 测试验证

### 6.1 功能测试

1. **连接测试**
   - 启动网关服务
   - 启动订单服务
   - 验证连接建立成功

2. **下单测试**
   ```bash
   curl -X POST http://localhost:8080/api/order/submit \
     -H "X-User-Id: user123" \
     -H "Content-Type: application/json" \
     -d '{
       "symbol": "BTCUSDT",
       "orderType": "LIMIT",
       "side": "BUY",
       "price": "50000.00",
       "quantity": "0.1"
     }'
   ```

3. **撤单测试**
   ```bash
   curl -X POST http://localhost:8080/api/order/cancel/ORD1704518400001abc123 \
     -H "X-User-Id: user123"
   ```

4. **成交通知测试**
   - 网关模拟发送成交通知
   - 验证订单状态更新
   - 验证资产变动

### 6.2 压力测试

- **并发连接**: 100个并发订单
- **消息吞吐**: 1000 TPS
- **响应时间**: P99 < 100ms

### 6.3 异常测试

1. **网关断线重连**
   - 关闭网关服务
   - 验证自动重连
   - 验证重连后消息正常

2. **消息丢失**
   - 模拟网络抖动
   - 验证消息重传机制

3. **格式错误**
   - 发送非法JSON
   - 验证错误处理不影响后续消息

## 7. 监控指标

### 7.1 关键指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| connection_status | 连接状态 | 断开超过1分钟 |
| message_send_count | 发送消息数 | - |
| message_receive_count | 接收消息数 | - |
| message_error_count | 消息错误数 | > 10/分钟 |
| response_time_p99 | 响应时间P99 | > 500ms |

### 7.2 日志规范

```java
// 连接日志
log.info("Gateway connection established: {}", channel.remoteAddress());

// 发送日志
log.debug("Message sent: msgType={}, msgId={}", message.getMsgType(), message.getMsgId());

// 接收日志
log.info("Received gateway message: type={}, msgId={}", msg.getMsgType(), msg.getMsgId());

// 错误日志
log.error("Failed to send message: msgId={}", message.getMsgId(), exception);
```

## 8. 配置参数

### 8.1 application.yml配置

```yaml
gateway:
  tcp:
    host: 127.0.0.1              # 网关地址
    port: 9900                    # 网关端口
    reconnect-interval: 5000      # 重连间隔（毫秒）
    connect-timeout: 10000        # 连接超时（毫秒）
    response-timeout: 10000       # 响应超时（毫秒）
    heartbeat-interval: 30000     # 心跳间隔（毫秒）
```

## 9. FAQ

### Q1: 如何保证消息不丢失？

**A**:
1. 使用TCP协议保证传输可靠性
2. 应用层保存已发送消息，等待响应确认
3. 超时重传机制
4. 重连后同步订单状态

### Q2: 网关重启后如何恢复？

**A**:
1. 订单服务检测到连接断开
2. 自动重连机制建立新连接
3. 从Redis读取未完成订单
4. 向网关查询订单状态（可选）

### Q3: 如何处理重复成交通知？

**A**:
1. 使用`tradeId`去重
2. Redis存储已处理的成交ID
3. 收到重复通知时直接忽略

### Q4: 消息顺序如何保证？

**A**:
1. TCP协议保证单连接内消息顺序
2. 使用单一长连接发送订单
3. 不同订单之间无顺序依赖

## 10. 联系方式

如有疑问，请联系：

- **技术支持**: tech@example.com
- **项目负责人**: Jay
- **文档版本**: v1.0
- **更新日期**: 2026-01-06
