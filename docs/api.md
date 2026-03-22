# Ultra Trading Order API

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
  "timestamp": 1742648000123
}
```

Rules:

- `code = 0` means success
- `code != 0` means failure
- Order and asset APIs require `X-User-Id`
- Symbol APIs do not require authentication headers

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

Description:

- Query all available symbols

Response `data`:

```json
[
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
    "updateTime": 1742648000123
  }
]
```

Default built-in symbols:

- `BTCUSDT`
- `ETHUSDT`
- `BNBUSDT`
- `SOLUSDT`
- `XRPUSDT`
- `DOGEUSDT`

### Get Symbol By Code

```http
GET /api/symbol/{symbol}
```

Path params:

- `symbol`: for example `BTCUSDT`

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": {
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
    "updateTime": 1742648000123
  },
  "timestamp": 1742648000456
}
```

Failure example:

```json
{
  "code": 404,
  "message": "Symbol not found",
  "data": null,
  "timestamp": 1742648000456
}
```

## Asset APIs

### Increase Asset

Test environment only.

```http
POST /api/asset/increase
```

Headers:

- `Content-Type: application/json`
- `X-User-Id: user001`

Request body:

```json
{
  "asset": "USDT",
  "amount": 100000,
  "description": "test recharge"
}
```

Fields:

- `asset`: required, asset code, will be normalized to uppercase
- `amount`: required, must be greater than `0`
- `description`: optional, flow description

Effects:

- increases `available`
- keeps `frozen` unchanged
- writes a `DEPOSIT` asset flow
- writes Redis cache
- persists asynchronously into MySQL

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": null,
    "userId": "user001",
    "asset": "USDT",
    "available": 100000,
    "frozen": 0,
    "updateTime": 1742648000123,
    "total": 100000
  },
  "timestamp": 1742648000456
}
```

Validation failure example:

```json
{
  "code": 400,
  "message": "Validation failed: {amount=Amount must be greater than 0}",
  "data": null,
  "timestamp": 1742648000456
}
```

### Get Single Asset Balance

```http
GET /api/asset/balance?asset=USDT
```

Headers:

- `X-User-Id: user001`

Query params:

- `asset`: required

Response `data`:

```json
{
  "id": null,
  "userId": "user001",
  "asset": "USDT",
  "available": 100000,
  "frozen": 0,
  "updateTime": 1742648000123,
  "total": 100000
}
```

### Get All Balances

```http
GET /api/asset/balances
```

Headers:

- `X-User-Id: user001`

Description:

- returns only assets where `total > 0`

### Get Asset Flows

```http
GET /api/asset/flow?asset=USDT&limit=100
```

Headers:

- `X-User-Id: user001`

Query params:

- `asset`: optional
- `limit`: optional, default `100`

Response item:

```json
{
  "flowId": "FLOW1742648000000abcd1234",
  "userId": "user001",
  "asset": "USDT",
  "flowType": "DEPOSIT",
  "amount": 100000,
  "balance": 100000,
  "relatedId": null,
  "description": "test recharge",
  "createTime": 1742648000123
}
```

## Order APIs

### Submit Order

```http
POST /api/order/submit
```

Headers:

- `Content-Type: application/json`
- `X-User-Id: user001`

Request body:

```json
{
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 50000,
  "quantity": 0.01,
  "clientOrderId": "test-001"
}
```

Fields:

- `symbol`: required
- `orderType`: required, `LIMIT` or `MARKET`
- `side`: required, `BUY` or `SELL`
- `price`: required for `LIMIT`
- `quantity`: required
- `clientOrderId`: optional

Current business rules:

- symbol must exist
- `LIMIT` order must include `price`
- `quantity` must be greater than or equal to `minOrderQty`
- buy order freezes quote asset
- sell order freezes base asset
- market buy is not supported yet
- order initial status is `PENDING`
- without external trade callbacks, the order will not move to `FILLED`

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": "ORD1742648000000abcd1234",
    "userId": "user001",
    "symbol": "BTCUSDT",
    "orderType": "LIMIT",
    "side": "BUY",
    "price": 50000,
    "quantity": 0.01,
    "filledQty": 0,
    "avgPrice": 0,
    "status": "PENDING",
    "createTime": 1742648000123,
    "updateTime": 1742648000123,
    "clientOrderId": "test-001"
  },
  "timestamp": 1742648000456
}
```

Typical failures:

- `Symbol not found: BTCUSDT`
- `Price is required for limit order`
- `Quantity below minimum: ...`
- `Insufficient balance: USDT`
- `Insufficient balance: BTC`

### Cancel Order

```http
POST /api/order/cancel/{orderId}
```

Headers:

- `X-User-Id: user001`

Path params:

- `orderId`

Success example:

```json
{
  "code": 0,
  "message": "success",
  "data": "Order cancel request submitted",
  "timestamp": 1742648000456
}
```

Failure cases:

- order not found
- order does not belong to current user
- order already filled or canceled

### Get Order List

```http
GET /api/order/list?symbol=BTCUSDT
```

Headers:

- `X-User-Id: user001`

Query params:

- `symbol`: optional

Response item:

```json
{
  "orderId": "ORD1742648000000abcd1234",
  "userId": "user001",
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 50000,
  "quantity": 0.01,
  "filledQty": 0,
  "avgPrice": 0,
  "status": "PENDING",
  "createTime": 1742648000123,
  "updateTime": 1742648000123,
  "clientOrderId": "test-001"
}
```

### Get Trade List By Order

```http
GET /api/order/trades/{orderId}
```

Headers:

- `X-User-Id: user001`

Path params:

- `orderId`

Response item:

```json
{
  "tradeId": "TRADE1742648000000abcd1234",
  "orderId": "ORD1742648000000abcd1234",
  "counterOrderId": "ORD1742647000000efgh5678",
  "userId": "user001",
  "symbol": "BTCUSDT",
  "price": 50000,
  "quantity": 0.01,
  "fee": 0.00001,
  "feeAsset": "BTC",
  "tradeTime": 1742648000123,
  "maker": false
}
```

Note:

- if there is no external EMS or trade callback, this list is usually empty

## Test Data

Recommended test users:

- `user001`
- `user002`

Recommended asset setup for `user001`:

- `USDT`: `100000`
- `BTC`: `1`
- `ETH`: `20`
- `SOL`: `500`

Recommended symbol data:

| Symbol | Min Qty | Tick Size | Step Size | Suggested Price |
| --- | ---: | ---: | ---: | ---: |
| BTCUSDT | 0.00001 | 0.01 | 0.00001 | 50000 |
| ETHUSDT | 0.0001 | 0.01 | 0.0001 | 3000 |
| BNBUSDT | 0.001 | 0.01 | 0.001 | 600 |
| SOLUSDT | 0.01 | 0.001 | 0.01 | 150 |
| XRPUSDT | 1 | 0.0001 | 1 | 0.60 |
| DOGEUSDT | 1 | 0.00001 | 1 | 0.12 |

Recommended order request examples:

Buy BTC:

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

Sell BTC:

```json
{
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "SELL",
  "price": 52000,
  "quantity": 0.005,
  "clientOrderId": "sell-btc-001"
}
```

Buy ETH:

```json
{
  "symbol": "ETHUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 3000,
  "quantity": 0.1,
  "clientOrderId": "buy-eth-001"
}
```

Buy XRP:

```json
{
  "symbol": "XRPUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": 0.6,
  "quantity": 100,
  "clientOrderId": "buy-xrp-001"
}
```

## Recommended Test Flow

1. Call `GET /api/symbol/list` to confirm default symbols are loaded.
2. Call `POST /api/asset/increase` to give `user001` enough balance.
3. Call `GET /api/asset/balances` to verify balances.
4. Call `POST /api/order/submit` for a `BTCUSDT` limit order.
5. Call `GET /api/order/list` to verify order persistence.
6. Call `POST /api/order/cancel/{orderId}` to verify cancel flow.
7. Call `GET /api/asset/flow` to verify deposit and freeze flows.
8. Call `GET /api/order/trades/{orderId}`. Without a trade callback, it is usually empty.

## Current Test Boundary

- `syncSymbolsFromBinance()` now uses local default symbols and does not rely on Binance network access.
- OKX sync is disabled in the task runner.
- Without external trade callbacks, orders will not be filled automatically.
- `POST /api/asset/increase` is for testing and should not be exposed directly in production.
