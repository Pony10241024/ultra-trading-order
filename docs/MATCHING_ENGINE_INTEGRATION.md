# 模拟撮合服务对接文档

## 1. 概述

本文档描述订单服务与模拟撮合引擎的对接规范，包括EMS（Execution Management System）消息推送和撮合逻辑说明。

### 1.1 架构图

```
┌──────────────┐    ZeroMQ PUSH      ┌──────────────┐
│              │  ──────────────────► │              │
│  订单服务    │                      │  EMS服务     │
│              │                      │              │
└──────────────┘                      └──────────────┘
       │                                      │
       │                                      ▼
       │                              ┌──────────────┐
       │                              │              │
       └────────────────────────────► │ 撮合引擎     │
                 订单/成交查询          │              │
                                      └──────────────┘
```

### 1.2 通信方式

- **消息队列**: ZeroMQ (JeroMQ)
- **模式**: PUSH-PULL
- **端点**: tcp://127.0.0.1:5555
- **序列化**: JSON

## 2. ZeroMQ消息协议

### 2.1 消息格式

```json
{
  "eventType": "事件类型",
  "orderId": "订单ID",
  "timestamp": 1704518400123,
  "data": "业务数据JSON字符串"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventType | String | 是 | 事件类型 |
| orderId | String | 是 | 订单ID |
| timestamp | Long | 是 | 事件时间戳（毫秒） |
| data | String | 是 | 业务数据JSON |

## 3. 事件类型定义

### 3.1 订单提交事件 (ORDER_SUBMIT)

**eventType**: `ORDER_SUBMIT`

**触发时机**: 订单服务收到下单请求并通过初步校验后

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
  "createTime": 1704518400123
}
```

**完整消息示例**:
```json
{
  "eventType": "ORDER_SUBMIT",
  "orderId": "ORD1704518400001abc123",
  "timestamp": 1704518400123,
  "data": "{\"orderId\":\"ORD1704518400001abc123\",\"userId\":\"user123\",\"symbol\":\"BTCUSDT\",\"orderType\":\"LIMIT\",\"side\":\"BUY\",\"price\":\"50000.00\",\"quantity\":\"0.1\",\"createTime\":1704518400123}"
}
```

**EMS处理逻辑**:
1. 接收订单提交事件
2. 记录订单提交日志
3. 推送到撮合引擎队列
4. 返回确认（可选）

### 3.2 订单撤销事件 (ORDER_CANCEL)

**eventType**: `ORDER_CANCEL`

**触发时机**: 订单服务收到撤单请求后

**data字段内容**:
```json
{
  "orderId": "ORD1704518400001abc123",
  "cancelTime": 1704518401000
}
```

**完整消息示例**:
```json
{
  "eventType": "ORDER_CANCEL",
  "orderId": "ORD1704518400001abc123",
  "timestamp": 1704518401000,
  "data": "{\"orderId\":\"ORD1704518400001abc123\",\"cancelTime\":1704518401000}"
}
```

**EMS处理逻辑**:
1. 接收撤单事件
2. 向撮合引擎发送撤单请求
3. 等待撮合引擎确认
4. 记录撤单日志

### 3.3 成交通知事件 (TRADE_FILLED)

**eventType**: `TRADE_FILLED`

**触发时机**: 订单服务收到网关成交通知后

**data字段内容**:
```json
{
  "tradeId": "TRD1704518400001xyz789",
  "orderId": "ORD1704518400001abc123",
  "userId": "user123",
  "symbol": "BTCUSDT",
  "price": "50000.00",
  "quantity": "0.05",
  "fee": "0.00005",
  "feeAsset": "BTC",
  "isMaker": true,
  "tradeTime": 1704518400500
}
```

**完整消息示例**:
```json
{
  "eventType": "TRADE_FILLED",
  "orderId": "ORD1704518400001abc123",
  "timestamp": 1704518400500,
  "data": "{\"tradeId\":\"TRD1704518400001xyz789\",\"orderId\":\"ORD1704518400001abc123\",\"userId\":\"user123\",\"symbol\":\"BTCUSDT\",\"price\":\"50000.00\",\"quantity\":\"0.05\",\"fee\":\"0.00005\",\"feeAsset\":\"BTC\",\"isMaker\":true,\"tradeTime\":1704518400500}"
}
```

**EMS处理逻辑**:
1. 接收成交通知
2. 更新订单成交状态
3. 记录成交明细
4. 触发风控检查（可选）
5. 推送给行情服务（可选）

## 4. 模拟撮合引擎实现

### 4.1 撮合引擎功能

撮合引擎负责：
1. 维护订单簿（Order Book）
2. 执行价格匹配逻辑
3. 生成成交记录
4. 推送成交通知

### 4.2 订单簿数据结构

```
BTCUSDT订单簿:

买单（Bids）- 按价格从高到低排列
┌────────┬─────────┬─────────┐
│  价格  │  数量   │ 订单数  │
├────────┼─────────┼─────────┤
│ 50100  │  0.5    │   3     │
│ 50050  │  1.2    │   5     │
│ 50000  │  2.0    │   8     │
└────────┴─────────┴─────────┘

卖单（Asks）- 按价格从低到高排列
┌────────┬─────────┬─────────┐
│  价格  │  数量   │ 订单数  │
├────────┼─────────┼─────────┤
│ 50200  │  1.5    │   6     │
│ 50250  │  0.8    │   4     │
│ 50300  │  2.5    │   7     │
└────────┴─────────┴─────────┘
```

### 4.3 撮合算法

**限价单撮合**:
```python
def match_limit_order(order):
    if order.side == BUY:
        # 买单：从最低卖单开始匹配
        for ask in asks:
            if ask.price <= order.price:
                # 价格满足，执行成交
                execute_trade(order, ask)
            else:
                break  # 价格不满足，停止匹配
    else:
        # 卖单：从最高买单开始匹配
        for bid in bids:
            if bid.price >= order.price:
                # 价格满足，执行成交
                execute_trade(order, bid)
            else:
                break
```

**市价单撮合**:
```python
def match_market_order(order):
    if order.side == BUY:
        # 市价买单：吃掉卖单直到数量满足
        for ask in asks:
            execute_trade(order, ask)
            if order.filled_quantity >= order.quantity:
                break
    else:
        # 市价卖单：吃掉买单直到数量满足
        for bid in bids:
            execute_trade(order, bid)
            if order.filled_quantity >= order.quantity:
                break
```

### 4.4 成交生成逻辑

```java
// 成交计算
Trade generateTrade(Order takerOrder, Order makerOrder) {
    Trade trade = new Trade();
    trade.setTradeId(generateTradeId());
    trade.setOrderId(takerOrder.getOrderId());
    trade.setPrice(makerOrder.getPrice());  // 成交价格为Maker价格

    // 计算成交数量
    BigDecimal quantity = takerOrder.getRemainingQuantity()
        .min(makerOrder.getRemainingQuantity());
    trade.setQuantity(quantity);

    // 计算手续费
    SymbolInfo symbolInfo = getSymbolInfo(takerOrder.getSymbol());
    BigDecimal feeRate = takerOrder.isMaker() ?
        symbolInfo.getMakerFee() : symbolInfo.getTakerFee();
    BigDecimal fee = quantity.multiply(feeRate);
    trade.setFee(fee);

    // 设置手续费币种
    String feeAsset = takerOrder.getSide() == BUY ?
        symbolInfo.getBaseAsset() : symbolInfo.getQuoteAsset();
    trade.setFeeAsset(feeAsset);

    trade.setMaker(takerOrder.isMaker());
    trade.setTradeTime(System.currentTimeMillis());

    return trade;
}
```

## 5. 模拟撮合引擎实现示例

### 5.1 简单撮合引擎（Python）

```python
import zmq
import json
from decimal import Decimal
from collections import defaultdict
import heapq

class MatchingEngine:
    def __init__(self):
        # 订单簿：symbol -> {bids: [], asks: []}
        self.order_books = defaultdict(lambda: {
            'bids': [],  # 买单（最大堆）
            'asks': []   # 卖单（最小堆）
        })

        # 订单字典：orderId -> Order
        self.orders = {}

        # ZeroMQ接收器
        self.context = zmq.Context()
        self.receiver = self.context.socket(zmq.PULL)
        self.receiver.bind("tcp://127.0.0.1:5555")

    def run(self):
        print("Matching engine started...")
        while True:
            # 接收消息
            message = self.receiver.recv_json()
            self.handle_message(message)

    def handle_message(self, message):
        event_type = message['eventType']

        if event_type == 'ORDER_SUBMIT':
            self.handle_order_submit(message)
        elif event_type == 'ORDER_CANCEL':
            self.handle_order_cancel(message)
        elif event_type == 'TRADE_FILLED':
            self.handle_trade_filled(message)

    def handle_order_submit(self, message):
        data = json.loads(message['data'])
        order = {
            'orderId': data['orderId'],
            'userId': data['userId'],
            'symbol': data['symbol'],
            'side': data['side'],
            'price': Decimal(data.get('price', '0')),
            'quantity': Decimal(data['quantity']),
            'filled': Decimal('0'),
            'orderType': data['orderType']
        }

        # 保存订单
        self.orders[order['orderId']] = order

        # 尝试撮合
        self.match_order(order)

        # 未完全成交的订单加入订单簿
        if order['filled'] < order['quantity']:
            self.add_to_order_book(order)

    def match_order(self, order):
        symbol = order['symbol']
        book = self.order_books[symbol]

        if order['side'] == 'BUY':
            # 买单：从卖单中匹配
            while book['asks'] and order['filled'] < order['quantity']:
                best_ask = heapq.heappop(book['asks'])

                # 检查价格是否匹配
                if order['orderType'] == 'LIMIT' and best_ask['price'] > order['price']:
                    # 价格不匹配，放回去
                    heapq.heappush(book['asks'], best_ask)
                    break

                # 执行成交
                self.execute_trade(order, best_ask)
        else:
            # 卖单：从买单中匹配
            while book['bids'] and order['filled'] < order['quantity']:
                best_bid = heapq.heappop(book['bids'])

                # 检查价格是否匹配
                if order['orderType'] == 'LIMIT' and best_bid['price'] < order['price']:
                    # 价格不匹配，放回去
                    heapq.heappush(book['bids'], best_bid)
                    break

                # 执行成交
                self.execute_trade(order, best_bid)

    def execute_trade(self, taker, maker):
        # 计算成交数量
        taker_remaining = taker['quantity'] - taker['filled']
        maker_remaining = maker['quantity'] - maker['filled']
        trade_quantity = min(taker_remaining, maker_remaining)

        # 更新成交数量
        taker['filled'] += trade_quantity
        maker['filled'] += trade_quantity

        # 生成成交记录
        trade = {
            'tradeId': f"TRD{int(time.time()*1000)}",
            'orderId': taker['orderId'],
            'price': str(maker['price']),
            'quantity': str(trade_quantity),
            'fee': str(trade_quantity * Decimal('0.001')),
            'feeAsset': taker['symbol'][:3],
            'isMaker': False
        }

        # 发送成交通知到网关（模拟）
        print(f"Trade executed: {trade}")

        # 如果Maker未完全成交，放回订单簿
        if maker['filled'] < maker['quantity']:
            if maker['side'] == 'BUY':
                heapq.heappush(self.order_books[maker['symbol']]['bids'], maker)
            else:
                heapq.heappush(self.order_books[maker['symbol']]['asks'], maker)

    def add_to_order_book(self, order):
        symbol = order['symbol']
        book = self.order_books[symbol]

        if order['side'] == 'BUY':
            # 买单：价格高的优先（最大堆，使用负价格）
            heapq.heappush(book['bids'], (-order['price'], order))
        else:
            # 卖单：价格低的优先（最小堆）
            heapq.heappush(book['asks'], (order['price'], order))

    def handle_order_cancel(self, message):
        data = json.loads(message['data'])
        order_id = data['orderId']

        if order_id in self.orders:
            order = self.orders[order_id]
            # 从订单簿移除（简化实现，实际需要更复杂的逻辑）
            print(f"Order canceled: {order_id}")
            del self.orders[order_id]

    def handle_trade_filled(self, message):
        # 记录成交
        print(f"Trade filled notification: {message}")

if __name__ == '__main__':
    engine = MatchingEngine()
    engine.run()
```

### 5.2 模拟C++网关（Python）

```python
import socket
import json
import struct
import threading
import time

class MockGateway:
    def __init__(self, host='127.0.0.1', port=9900):
        self.host = host
        self.port = port
        self.server_socket = None
        self.client_socket = None

    def start(self):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(1)

        print(f"Mock gateway started on {self.host}:{self.port}")

        # 等待连接
        self.client_socket, addr = self.server_socket.accept()
        print(f"Client connected from {addr}")

        # 启动消息处理线程
        threading.Thread(target=self.handle_messages, daemon=True).start()

        # 主线程：模拟成交推送
        self.simulate_trades()

    def handle_messages(self):
        while True:
            try:
                # 读取消息
                message = self.receive_message()
                if not message:
                    break

                print(f"Received: {message}")

                # 根据消息类型响应
                msg_type = message['msgType']

                if msg_type == 'ORDER_REQUEST':
                    self.handle_order_request(message)
                elif msg_type == 'CANCEL_REQUEST':
                    self.handle_cancel_request(message)

            except Exception as e:
                print(f"Error: {e}")
                break

    def receive_message(self):
        # 读取长度（4字节）
        length_bytes = self.client_socket.recv(4)
        if not length_bytes:
            return None

        length = struct.unpack('!I', length_bytes)[0]

        # 读取消息体
        data = b''
        while len(data) < length:
            chunk = self.client_socket.recv(length - len(data))
            if not chunk:
                return None
            data += chunk

        # 解析JSON
        return json.loads(data.decode('utf-8'))

    def send_message(self, message):
        # 序列化为JSON
        json_str = json.dumps(message)
        data = json_str.encode('utf-8')

        # 发送长度
        length = struct.pack('!I', len(data))
        self.client_socket.send(length)

        # 发送数据
        self.client_socket.send(data)

    def handle_order_request(self, message):
        data = json.loads(message['data'])
        order_id = data['orderId']

        # 模拟处理延迟
        time.sleep(0.1)

        # 发送下单响应
        response = {
            'msgType': 'ORDER_RESPONSE',
            'msgId': f"resp_{message['msgId']}",
            'timestamp': int(time.time() * 1000),
            'data': json.dumps({
                'orderId': order_id,
                'status': 'SUBMITTED',
                'gatewayOrderId': f"GW_{order_id}",
                'message': 'Order submitted successfully'
            })
        }

        self.send_message(response)
        print(f"Sent order response for {order_id}")

    def handle_cancel_request(self, message):
        data = json.loads(message['data'])
        order_id = data['orderId']

        # 模拟处理延迟
        time.sleep(0.05)

        # 发送撤单响应
        response = {
            'msgType': 'CANCEL_RESPONSE',
            'msgId': f"resp_{message['msgId']}",
            'timestamp': int(time.time() * 1000),
            'data': json.dumps({
                'orderId': order_id,
                'success': True,
                'message': 'Order canceled successfully'
            })
        }

        self.send_message(response)
        print(f"Sent cancel response for {order_id}")

    def simulate_trades(self):
        # 模拟周期性成交推送
        while True:
            time.sleep(5)  # 每5秒推送一次

            # 模拟成交通知
            trade_notify = {
                'msgType': 'TRADE_NOTIFY',
                'msgId': f"trade_{int(time.time()*1000)}",
                'timestamp': int(time.time() * 1000),
                'data': json.dumps({
                    'orderId': 'ORD_SAMPLE',
                    'tradeId': f"TRD_{int(time.time()*1000)}",
                    'price': '50000.00',
                    'quantity': '0.01',
                    'fee': '0.00001',
                    'feeAsset': 'BTC',
                    'isMaker': True,
                    'tradeTime': int(time.time() * 1000)
                })
            }

            try:
                self.send_message(trade_notify)
                print("Sent simulated trade notification")
            except Exception as e:
                print(f"Failed to send trade: {e}")
                break

if __name__ == '__main__':
    gateway = MockGateway()
    gateway.start()
```

## 6. 测试流程

### 6.1 环境准备

```bash
# 1. 启动Redis
redis-server

# 2. 启动模拟撮合引擎（Python）
python matching_engine.py

# 3. 启动模拟网关（Python）
python mock_gateway.py

# 4. 启动订单服务（Java）
mvn spring-boot:run
```

### 6.2 功能测试

**测试1：下单并成交**
```bash
# 1. 下买单
curl -X POST http://localhost:8080/api/order/submit \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT",
    "orderType": "LIMIT",
    "side": "BUY",
    "price": "50000.00",
    "quantity": "0.1"
  }'

# 2. 下卖单（应该会成交）
curl -X POST http://localhost:8080/api/order/submit \
  -H "X-User-Id: user2" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT",
    "orderType": "LIMIT",
    "side": "SELL",
    "price": "50000.00",
    "quantity": "0.1"
  }'

# 3. 查询成交记录
curl http://localhost:8080/api/order/list?symbol=BTCUSDT \
  -H "X-User-Id: user1"
```

**测试2：撤单**
```bash
# 1. 下单
ORDER_ID=$(curl -X POST http://localhost:8080/api/order/submit \
  -H "X-User-Id: user1" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT",
    "orderType": "LIMIT",
    "side": "BUY",
    "price": "49000.00",
    "quantity": "0.1"
  }' | jq -r '.data.orderId')

# 2. 撤单
curl -X POST http://localhost:8080/api/order/cancel/$ORDER_ID \
  -H "X-User-Id: user1"

# 3. 验证订单状态
curl http://localhost:8080/api/order/list \
  -H "X-User-Id: user1"
```

## 7. 监控和日志

### 7.1 EMS监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| message_received_count | 接收消息数 | - |
| message_processed_count | 处理消息数 | - |
| message_error_count | 消息错误数 | > 10/分钟 |
| matching_latency | 撮合延迟 | > 100ms |
| order_book_depth | 订单簿深度 | - |

### 7.2 日志示例

```
[EMS] 2026-01-06 12:00:00.123 INFO  - Received ORDER_SUBMIT event: orderId=ORD123
[EMS] 2026-01-06 12:00:00.125 INFO  - Order added to matching queue: orderId=ORD123
[EMS] 2026-01-06 12:00:00.128 INFO  - Trade executed: tradeId=TRD456, orderId=ORD123, price=50000.00, qty=0.05
[EMS] 2026-01-06 12:00:00.130 INFO  - Trade notification sent: tradeId=TRD456
```

## 8. FAQ

### Q1: ZeroMQ消息会丢失吗？

**A**:
- ZeroMQ默认有内存队列缓冲
- 可配置持久化到磁盘
- 建议在接收端实现ACK机制

### Q2: 撮合引擎如何保证公平性？

**A**:
- 价格优先：更优价格优先成交
- 时间优先：相同价格先到先得
- 使用时间戳排序保证FIFO

### Q3: 如何处理成交通知延迟？

**A**:
- 设置超时机制
- 实现补偿查询接口
- 定期对账

## 9. 附录

### 9.1 完整消息流程图

```
订单服务                ZeroMQ               撮合引擎              网关
   │                     │                     │                  │
   │──ORDER_SUBMIT──────►│                     │                  │
   │                     │──ORDER_SUBMIT──────►│                  │
   │                     │                     │──撮合处理──►     │
   │                     │                     │◄─成交结果───     │
   │                     │◄──TRADE_NOTIFY──────│                  │
   │                     │                     │──TRADE_NOTIFY───►│
   │◄──TRADE_NOTIFY─────│                     │                  │
   │                     │                     │                  │
```

### 9.2 性能优化建议

1. **批量处理**: 批量发送ZeroMQ消息
2. **异步处理**: 使用异步IO提升吞吐
3. **内存优化**: 限制订单簿深度
4. **负载均衡**: 多个撮合引擎实例

## 10. 联系方式

如有疑问，请联系：

- **技术支持**: tech@example.com
- **项目负责人**: Jay
- **文档版本**: v1.0
- **更新日期**: 2026-01-06
