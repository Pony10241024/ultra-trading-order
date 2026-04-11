# Trading Order 对外 TCP 接口文档

**版本**: 1.0.0
**更新日期**: 2026-03-25

## 1. 连接信息

- 服务地址: `116.63.80.121:5502`
- 传输协议: TCP
- 编码: UTF-8
- 报文协议: `4` 字节大端长度前缀 + JSON 消息体

## 2. 报文格式

### 2.1 外层消息结构

```json
{
  "msgType": "ORDER_REQUEST",
  "msgId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1742860800000,
  "data": "{\"mainAccountId\":\"main001\",\"tradeAccount\":\"trade001\",\"symbol\":\"BTCUSDT\"}"
}
```

字段说明:

- `msgType`: 消息类型，请求和响应成对出现
- `msgId`: 消息唯一标识，建议使用 UUID，请求与响应一一对应
- `timestamp`: 毫秒时间戳
- `data`: 业务 JSON 字符串

### 2.2 传输格式

```text
+----------------+-----------------------------------+
|  4 Bytes (Int) |      N Bytes (UTF-8 JSON)        |
|   消息体长度    |            消息体                 |
+----------------+-----------------------------------+
```

- 长度字段为 4 字节大端整数
- 长度值表示后续 JSON 字节数

## 3. 账户字段

- `mainAccountId`: 主账户 ID
- `tradeAccount`: 交易账户 ID，业务唯一标识

兼容说明:

- 当前仍兼容 `userId`
- 如果仅传 `userId`，服务端会将其同时映射为 `mainAccountId` 和 `tradeAccount`

## 4. 统一响应格式

所有 TCP 响应的 `data` 字段中，都是统一的业务响应体:

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1742860800123
}
```

返回码说明:

- `0`: 成功
- `404`: 资源不存在
- `9999`: 业务异常或参数错误

## 5. 消息类型

### 5.1 获取交易对列表

`msgType`: `SYMBOL_LIST_REQUEST`

请求 `data`:

```json
{}
```

响应 `data.data`: `SymbolInfo[]`

### 5.2 获取单个交易对信息

`msgType`: `SYMBOL_INFO_REQUEST`

请求 `data`:

```json
{
  "symbol": "BTCUSDT"
}
```

响应 `data.data`: `SymbolInfo`

### 5.3 查询单个资产余额

`msgType`: `ASSET_BALANCE_REQUEST`

请求 `data`:

```json
{
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT"
}
```

响应 `data.data`: `Balance`

### 5.4 查询全部资产余额

`msgType`: `ASSET_BALANCES_REQUEST`

请求 `data`:

```json
{
  "mainAccountId": "main001",
  "tradeAccount": "trade001"
}
```

响应 `data.data`: `Balance[]`

### 5.5 查询资产流水

`msgType`: `ASSET_FLOW_REQUEST`

请求 `data`:

```json
{
  "tradeAccount": "trade001",
  "asset": "USDT",
  "limit": 100
}
```

字段说明:

- `asset`: 可选，不传表示查询全部资产流水
- `limit`: 可选，默认 `100`

响应 `data.data`: `AssetFlow[]`

### 5.6 下单

`msgType`: `ORDER_REQUEST`

请求 `data`:

```json
{
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 50000,
  "quantity": 0.01,
  "clientOrderId": "buy-btc-001"
}
```

字段说明:

- `symbol`: 交易对，例如 `BTCUSDT`
- `orderType`: `LIMIT` 或 `MARKET`
- `side`: `BUY` 或 `SELL`
- `price`: 限价单必填，市价单可不传
- `quantity`: 下单数量
- `clientOrderId`: 可选，客户端自定义订单号

响应 `data.data`: `Order`

### 5.7 撤单

`msgType`: `CANCEL_REQUEST`

请求 `data`:

```json
{
  "tradeAccount": "trade001",
  "orderId": "ORD1742860800000abcd1234"
}
```

响应 `data.data` 示例:

```json
{
  "orderId": "ORD1742860800000abcd1234",
  "status": "CANCELED",
  "message": "Cancel request submitted"
}
```

### 5.8 查询订单列表

`msgType`: `ORDER_LIST_REQUEST`

请求 `data`:

```json
{
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT"
}
```

字段说明:

- `symbol`: 可选，不传表示查询该账户全部订单

响应 `data.data`: `Order[]`

### 5.9 查询当日订单

`msgType`: `ORDER_TODAY_REQUEST`

按 `UTC+0` 自然日查询当日订单，`createTime` 为毫秒时间戳，结果按 `createTime` 倒序返回。

请求 `data`:

```json
{
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT"
}
```

字段说明:

- `symbol`: 可选，不传表示查询该账户在 `UTC+0` 当天的全部订单

响应 `data.data`: `Order[]`

### 5.10 查询订单成交列表

`msgType`: `TRADE_LIST_REQUEST`

请求 `data`:

```json
{
  "tradeAccount": "trade001",
  "orderId": "ORD1742860800000abcd1234"
}
```

响应 `data.data`: `Trade[]`

## 6. 数据结构示例

### 6.1 Balance

```json
{
  "id": null,
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT",
  "available": 100000,
  "frozen": 0,
  "updateTime": 1742860800123,
  "total": 100000
}
```

### 6.2 Order

```json
{
  "orderId": "ORD1742860800000abcd1234",
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 50000,
  "quantity": 0.01,
  "filledQty": 0,
  "avgPrice": 0,
  "status": "PENDING",
  "createTime": 1742860800123,
  "updateTime": 1742860800123,
  "clientOrderId": "buy-btc-001"
}
```

### 6.3 Trade

```json
{
  "tradeId": "TRADE1742860800000abcd1234",
  "orderId": "ORD1742860800000abcd1234",
  "counterOrderId": "ORD1742860700000efgh5678",
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT",
  "price": 50000,
  "quantity": 0.01,
  "fee": 0.00001,
  "feeAsset": "BTC",
  "tradeTime": 1742860800123,
  "maker": false
}
```

### 6.4 AssetFlow

```json
{
  "flowId": "FLOW1742860800000abcd1234",
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT",
  "flowType": "DEPOSIT",
  "amount": 100000,
  "balance": 100000,
  "relatedId": null,
  "description": "test recharge",
  "createTime": 1742860800123
}
```

## 7. 完整请求示例

### 7.1 下单请求

```json
{
  "msgType": "ORDER_REQUEST",
  "msgId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1742860800000,
  "data": "{\"mainAccountId\":\"main001\",\"tradeAccount\":\"trade001\",\"symbol\":\"BTCUSDT\",\"orderType\":\"LIMIT\",\"side\":\"BUY\",\"price\":50000,\"quantity\":0.01,\"clientOrderId\":\"buy-btc-001\"}"
}
```

### 7.2 查询当日订单请求

```json
{
  "msgType": "ORDER_TODAY_REQUEST",
  "msgId": "660e8400-e29b-41d4-a716-446655440001",
  "timestamp": 1742860800000,
  "data": "{\"tradeAccount\":\"trade001\"}"
}
```

## 8. 说明

- 本文档仅包含对外 TCP 接口
- 不提供 `ASSET_INCREASE_REQUEST` 加资产接口
- 所有时间字段均为毫秒时间戳
- 查询“当日订单”时，日期边界按 `UTC+0` 计算
