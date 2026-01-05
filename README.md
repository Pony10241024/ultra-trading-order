# Ultra Trading Order Service

极速交易订单+资产管理服务

## 项目简介

基于Spring Boot 3.x + Maven构建的高性能交易系统，实现订单管理和资产管理功能。

### 核心功能

1. **订单管理**
   - 限价单/市价单下单
   - 订单撤销
   - 订单列表查询
   - 成交记录查询

2. **资产管理**
   - 余额查询
   - 资金冻结/解冻
   - 资金流水记录

3. **交易对管理**
   - 从币安/OKX自动获取交易对信息
   - 定时更新交易对配置
   - 最小下单量、手续费等参数管理

4. **网关对接**
   - 基于Netty的TCP客户端连接C++网关
   - 自动重连机制
   - 消息编解码

5. **EMS服务交互**
   - 基于ZeroMQ的消息推送
   - 下单/撤单/成交事件通知

## 技术栈

- **框架**: Spring Boot 3.2.1
- **JDK**: Java 17
- **构建工具**: Maven
- **Redis客户端**: Redisson 3.25.2
- **网络通信**: Netty 4.1.104
- **消息队列**: JeroMQ 0.6.0
- **HTTP客户端**: Spring WebFlux
- **工具库**: Lombok, Guava, Jackson

## 项目结构

```
src/main/java/com/uex/trading/
├── TradingApplication.java          # 启动类
├── asset/                           # 资产模块
│   ├── Balance.java                 # 余额实体
│   ├── AssetFlow.java               # 资金流水
│   └── AssetService.java            # 资产服务
├── order/                           # 订单模块
│   ├── Order.java                   # 订单实体
│   ├── Trade.java                   # 成交记录
│   ├── OrderRequest.java            # 下单请求
│   └── OrderService.java            # 订单服务
├── symbol/                          # 交易对模块
│   ├── SymbolInfo.java              # 交易对信息
│   ├── SymbolService.java           # 交易对服务
│   └── SymbolSyncTask.java          # 定时同步任务
├── gateway/                         # 网关通信模块
│   ├── GatewayTcpClient.java        # TCP客户端
│   ├── GatewayMessage.java          # 网关消息
│   ├── GatewayMessageEncoder.java   # 消息编码器
│   ├── GatewayMessageDecoder.java   # 消息解码器
│   ├── GatewayMessageHandler.java   # 消息处理器
│   └── GatewayResponseDispatcher.java # 响应分发器
├── zeromq/                          # ZeroMQ模块
│   ├── ZeroMqClient.java            # ZeroMQ客户端
│   └── EmsMessage.java              # EMS消息
├── controller/                      # REST API控制器
│   ├── OrderController.java         # 订单接口
│   ├── AssetController.java         # 资产接口
│   └── SymbolController.java        # 交易对接口
├── config/                          # 配置类
│   ├── RedisConfig.java             # Redis配置
│   ├── WebClientConfig.java         # WebClient配置
│   └── GlobalExceptionHandler.java  # 全局异常处理
└── common/                          # 通用类
    ├── OrderType.java               # 订单类型枚举
    ├── OrderSide.java               # 买卖方向枚举
    ├── OrderStatus.java             # 订单状态枚举
    ├── FlowType.java                # 流水类型枚举
    └── ApiResponse.java             # 统一响应格式
```

## 配置说明

### application.yml

```yaml
# 服务端口
server:
  port: 8080

# Redis配置
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://127.0.0.1:6379"
          database: 0

# 网关TCP配置
gateway:
  tcp:
    host: 127.0.0.1
    port: 9900
    reconnect-interval: 5000

# ZeroMQ配置
zeromq:
  ems:
    endpoint: "tcp://127.0.0.1:5555"

# 交易所API
exchange:
  binance:
    api-url: "https://api.binance.com"
  okx:
    api-url: "https://www.okx.com"

# 交易对同步
symbol:
  sync:
    enabled: true
    fixed-rate: 300000  # 5分钟
```

## API接口

### 订单接口

#### 下单
```
POST /api/order/submit
Header: X-User-Id: user123
Body:
{
  "symbol": "BTCUSDT",
  "orderType": "LIMIT",
  "side": "BUY",
  "price": "50000.00",
  "quantity": "0.1"
}
```

#### 撤单
```
POST /api/order/cancel/{orderId}
Header: X-User-Id: user123
```

#### 订单列表
```
GET /api/order/list?symbol=BTCUSDT
Header: X-User-Id: user123
```

#### 成交列表
```
GET /api/order/trades/{orderId}
Header: X-User-Id: user123
```

### 资产接口

#### 查询余额
```
GET /api/asset/balance?asset=USDT
Header: X-User-Id: user123
```

#### 查询所有余额
```
GET /api/asset/balances
Header: X-User-Id: user123
```

#### 资金流水
```
GET /api/asset/flow?asset=USDT&limit=100
Header: X-User-Id: user123
```

### 交易对接口

#### 查询交易对信息
```
GET /api/symbol/BTCUSDT
```

#### 查询所有交易对
```
GET /api/symbol/list
```

## 网关协议

### 消息格式
```json
{
  "msgType": "ORDER_REQUEST|ORDER_RESPONSE|CANCEL_REQUEST|CANCEL_RESPONSE|TRADE_NOTIFY",
  "msgId": "唯一消息ID",
  "timestamp": 1234567890,
  "data": "JSON格式数据"
}
```

### 传输协议
- 4字节长度（大端序）+ JSON数据（UTF-8编码）

## ZeroMQ消息

### EMS消息格式
```json
{
  "eventType": "ORDER_SUBMIT|ORDER_CANCEL|TRADE_FILLED",
  "orderId": "订单ID",
  "timestamp": 1234567890,
  "data": "JSON格式详细数据"
}
```

## Redis数据结构

### 订单数据
- `trading:order:map` - Hash: orderId -> Order对象
- `trading:order:user:{userId}` - List: 用户订单ID列表

### 成交数据
- `trading:trade:order:{orderId}` - List: 订单成交列表
- `trading:trade:user:{userId}` - List: 用户成交列表

### 资产数据
- `trading:balance:{userId}:{asset}` - Hash: 余额信息
- `trading:flow:user:{userId}` - List: 用户资金流水
- `trading:flow:user:{userId}:{asset}` - List: 指定币种流水

### 交易对数据
- `trading:symbol:map` - Hash: symbol -> SymbolInfo对象

## 运行方式

### 前置条件
1. Redis服务运行在 127.0.0.1:6379
2. C++网关服务运行在 127.0.0.1:9900
3. EMS服务ZeroMQ端点 tcp://127.0.0.1:5555

### 启动服务
```bash
mvn clean package
java -jar target/ultra-trading-order-1.0.0-SNAPSHOT.jar
```

或使用Maven直接运行:
```bash
mvn spring-boot:run
```

## 开发计划

### 当前版本 (v1.0)
- ✅ 订单管理（限价/市价下单、撤单）
- ✅ 资产管理（余额、流水）
- ✅ 网关TCP对接
- ✅ EMS ZeroMQ通信
- ✅ 交易对信息管理
- ✅ 数据存储Redis

### 下一版本 (v2.0)
- [ ] 数据持久化到数据库（MySQL/PostgreSQL）
- [ ] 订单状态机优化
- [ ] 风控模块
- [ ] 行情推送
- [ ] WebSocket实时推送
- [ ] 性能优化和压测

## 注意事项

1. **数据存储**: 当前版本所有数据存储在Redis，重启后数据会保留（取决于Redis持久化配置）
2. **网关协议**: 需要与C++网关协商具体的消息格式和字段
3. **异常处理**: 网关断线会自动重连，但已发送的消息不会重发
4. **并发控制**: 使用Redisson的分布式锁保证并发安全
5. **手续费计算**: 从交易对配置中获取，实际成交时扣除

## License

MIT License
