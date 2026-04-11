# Ultra Trading Order TCP API

## 1. Overview

TCP server:

```text
127.0.0.1:5502
```

Protocol:

- 4-byte big-endian length prefix
- followed by UTF-8 JSON payload

Outer message format:

```json
{
  "msgType": "ORDER_REQUEST",
  "msgId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1742736000000,
  "data": "{\"mainAccountId\":\"main001\",\"tradeAccount\":\"trade001\",\"symbol\":\"BTCUSDT\"}"
}
```

Fields:

- `msgType`: request or response message type
- `msgId`: request id, echoed back in response
- `timestamp`: milliseconds
- `data`: business JSON string

Account model:

- `mainAccountId`: 所属主账户，长度 16
- `tradeAccount`: 交易账号，长度 16，业务唯一标识

Compatibility:

- `userId` is still accepted temporarily in TCP `data`
- if only `userId` is provided, the server will map it to both `mainAccountId` and `tradeAccount`

## 2. Unified Response

All TCP responses store a unified business body in `data`:

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1742736000123
}
```

Rules:

- `code = 0` means success
- `code = 404` means resource not found
- `code = 9999` means business error or bad request

## 3. Message Types

### SYMBOL_LIST_REQUEST

Request `data`:

```json
{}
```

Response `data.data`: `SymbolInfo[]`

### SYMBOL_INFO_REQUEST

Request `data`:

```json
{
  "symbol": "BTCUSDT"
}
```

### ASSET_INCREASE_REQUEST

Request `data`:

```json
{
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT",
  "amount": 100000,
  "description": "test recharge"
}
```

### ASSET_BALANCE_REQUEST

Request `data`:

```json
{
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT"
}
```

### ASSET_BALANCES_REQUEST

Request `data`:

```json
{
  "mainAccountId": "main001",
  "tradeAccount": "trade001"
}
```

### ASSET_FLOW_REQUEST

Request `data`:

```json
{
  "tradeAccount": "trade001",
  "asset": "USDT",
  "limit": 100
}
```

### ORDER_REQUEST

Request `data`:

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

Request field constraints:

- `symbol`: `string`，交易对，例如 `BTCUSDT`
- `orderType`: `enum(OrderType)`，可选值：`LIMIT`、`MARKET`
- `side`: `enum(OrderSide)`，可选值：`BUY`、`SELL`
- `price`: `number`，限价单必填，市价单可不传
- `quantity`: `number`
- `clientOrderId`: `string`，可选

Response `data.data`: complete `Order` object.

### CANCEL_REQUEST

Request `data`:

```json
{
  "tradeAccount": "trade001",
  "orderId": "ORD1742736000000abcd1234"
}
```

### ORDER_LIST_REQUEST

Request `data`:

```json
{
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT"
}
```

### ORDER_TODAY_REQUEST

按 `UTC+0` 自然日查询当日订单，`createTime` 使用毫秒时间戳，按 `createTime` 倒序返回。

Request `data`:

```json
{
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT"
}
```

### TRADE_LIST_REQUEST

Request `data`:

```json
{
  "tradeAccount": "trade001",
  "orderId": "ORD1742736000000abcd1234"
}
```

## 4. Common Structures

### Balance

```json
{
  "id": null,
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT",
  "available": 100000,
  "frozen": 0,
  "updateTime": 1742736000123,
  "total": 100000
}
```

### Order

```json
{
  "orderId": "ORD1742736000000abcd1234",
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
  "createTime": 1742736000123,
  "updateTime": 1742736000123,
  "clientOrderId": "buy-btc-001"
}
```

### Trade

```json
{
  "tradeId": "TRADE1742736000000abcd1234",
  "orderId": "ORD1742736000000abcd1234",
  "counterOrderId": "ORD1742735000000efgh5678",
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "symbol": "BTCUSDT",
  "price": 50000,
  "quantity": 0.01,
  "fee": 0.00001,
  "feeAsset": "BTC",
  "tradeTime": 1742736000123,
  "maker": false
}
```

### AssetFlow

```json
{
  "flowId": "FLOW1742736000000abcd1234",
  "mainAccountId": "main001",
  "tradeAccount": "trade001",
  "asset": "USDT",
  "flowType": "DEPOSIT",
  "amount": 100000,
  "balance": 100000,
  "relatedId": null,
  "description": "test recharge",
  "createTime": 1742736000123
}
```

## 5. Local Smoke Test Script

Built-in script:

- [scripts/tcp_smoke_test.py](/Users/jay/IdeaProjects/uex/ultra-trading-order/scripts/tcp_smoke_test.py)

Run after the service is up:

```bash
python3 scripts/tcp_smoke_test.py
```

Custom example:

```bash
python3 scripts/tcp_smoke_test.py \
  --host 127.0.0.1 \
  --port 5502 \
  --main-account-id main001 \
  --trade-account trade001 \
  --symbol BTCUSDT \
  --asset USDT \
  --amount 100000 \
  --price 50000 \
  --quantity 0.01
```
