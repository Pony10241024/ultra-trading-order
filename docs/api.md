# Ultra Trading Order HTTP API

## Overview

Base URL:

```text
http://127.0.0.1:8080
```

Unified response format:

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
- `code != 0` means failure
- Order and asset APIs require account identity
- Preferred headers:
  - `X-Main-Account-Id`
  - `X-Trade-Account`
- Backward compatibility:
  - `X-User-Id` is still accepted temporarily, and will be used as both `mainAccountId` and `tradeAccount`

User identity model:

- `mainAccountId`: 所属主账户，长度 16
- `tradeAccount`: 交易账号，长度 16，作为业务唯一账户标识

Enums:

- `OrderType`: `LIMIT`, `MARKET`
- `OrderSide`: `BUY`, `SELL`
- `OrderStatus`: `PENDING`, `SUBMITTED`, `PARTIAL_FILLED`, `FILLED`, `CANCELED`, `REJECTED`
- `FlowType`: `DEPOSIT`, `WITHDRAW`, `TRADE_IN`, `TRADE_OUT`, `FEE`

## Symbol APIs

### Get All Symbols

```http
GET /api/symbol/list
```

No auth headers required.

Response item:

```json
{
  "symbol": "BTCUSDT",
  "baseAsset": "BTC",
  "quoteAsset": "USDT",
  "minOrderAmount": 10,
  "minOrderQty": 0.00001,
  "tickSize": 0.01,
  "stepSize": 0.00001,
  "makerFee": 0.001,
  "takerFee": 0.001,
  "exchange": "BINANCE",
  "updateTime": 1742736000123
}
```

### Get Symbol By Code

```http
GET /api/symbol/{symbol}
```

Path params:

- `symbol`: for example `BTCUSDT`

## Asset APIs

### Increase Asset

Test environment only.

```http
POST /api/asset/increase
```

Headers:

- `Content-Type: application/json`
- `X-Main-Account-Id: main001`
- `X-Trade-Account: trade001`

Request body:

```json
{
  "asset": "USDT",
  "amount": 100000,
  "description": "test recharge"
}
```

Success `data`:

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

### Get Single Asset Balance

```http
GET /api/asset/balance?asset=USDT
```

Headers:

- `X-Main-Account-Id: main001`
- `X-Trade-Account: trade001`

### Get All Balances

```http
GET /api/asset/balances
```

Headers:

- `X-Main-Account-Id: main001`
- `X-Trade-Account: trade001`

### Get Asset Flows

```http
GET /api/asset/flow?asset=USDT&limit=100
```

Headers:

- `X-Trade-Account: trade001`

Response item:

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

## Order APIs

### Submit Order

```http
POST /api/order/submit
```

Headers:

- `Content-Type: application/json`
- `X-Main-Account-Id: main001`
- `X-Trade-Account: trade001`

Request body:

```json
{
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 50000,
  "quantity": 0.01,
  "clientOrderId": "buy-btc-001"
}
```

Request fields:

- `symbol`: `string`，交易对，例如 `BTCUSDT`
- `orderType`: `enum(OrderType)`，可选值：`LIMIT`、`MARKET`
- `side`: `enum(OrderSide)`，可选值：`BUY`、`SELL`
- `price`: `number`，限价单必填，市价单可不传
- `quantity`: `number`
- `clientOrderId`: `string`，可选

Success `data`:

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

### Cancel Order

```http
POST /api/order/cancel/{orderId}
```

Headers:

- `X-Trade-Account: trade001`

### Get Order List

```http
GET /api/order/list?symbol=BTCUSDT
```

Headers:

- `X-Trade-Account: trade001`

### Get Today Order List

按照 `UTC+0` 自然日查询当日订单，`createTime` 使用毫秒时间戳，按 `createTime` 倒序返回。

```http
GET /api/order/today?symbol=BTCUSDT
```

Headers:

- `X-Trade-Account: trade001`

### Get Trade List By Order

```http
GET /api/order/trades/{orderId}
```

Headers:

- `X-Trade-Account: trade001`

Response item:

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

## Test Data

Recommended test identity:

- `mainAccountId = main001`
- `tradeAccount = trade001`

Recommended funding:

- `USDT = 100000`
- `BTC = 1`
- `ETH = 20`

Recommended symbols:

- `BTCUSDT`
- `ETHUSDT`
- `BNBUSDT`
- `SOLUSDT`
- `XRPUSDT`
- `DOGEUSDT`
