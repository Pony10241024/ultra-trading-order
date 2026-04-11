-- Ultra Trading Order Service
-- Production initialization script
-- Table prefix: tb_

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_order` (
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  `symbol` varchar(32) NOT NULL COMMENT '交易对',
  `order_type` varchar(16) NOT NULL COMMENT '订单类型: LIMIT, MARKET',
  `side` varchar(8) NOT NULL COMMENT '买卖方向: BUY, SELL',
  `price` decimal(32,16) DEFAULT NULL COMMENT '价格',
  `quantity` decimal(32,16) NOT NULL COMMENT '数量',
  `filled_qty` decimal(32,16) DEFAULT NULL COMMENT '已成交数量',
  `avg_price` decimal(32,16) DEFAULT NULL COMMENT '平均成交价格',
  `status` varchar(16) NOT NULL COMMENT '订单状态: PENDING, SUBMITTED, PARTIAL_FILLED, FILLED, CANCELED, REJECTED',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  `client_order_id` varchar(64) DEFAULT NULL COMMENT '客户端订单ID',
  PRIMARY KEY (`order_id`),
  KEY `idx_main_account_id` (`main_account_id`),
  KEY `idx_trade_account` (`trade_account`),
  KEY `idx_symbol` (`symbol`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `tb_trade` (
  `trade_id` varchar(64) NOT NULL COMMENT '成交ID',
  `order_id` varchar(64) NOT NULL COMMENT '订单ID',
  `counter_order_id` varchar(64) DEFAULT NULL COMMENT '对手订单ID',
  `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  `symbol` varchar(32) NOT NULL COMMENT '交易对',
  `price` decimal(32,16) NOT NULL COMMENT '成交价格',
  `quantity` decimal(32,16) NOT NULL COMMENT '成交数量',
  `fee` decimal(32,16) DEFAULT NULL COMMENT '手续费',
  `fee_asset` varchar(16) DEFAULT NULL COMMENT '手续费币种',
  `trade_time` bigint NOT NULL COMMENT '成交时间',
  `is_maker` bit(1) DEFAULT NULL COMMENT '是否为Maker',
  PRIMARY KEY (`trade_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_main_account_id` (`main_account_id`),
  KEY `idx_trade_account` (`trade_account`),
  KEY `idx_trade_time` (`trade_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交表';

CREATE TABLE IF NOT EXISTS `tb_balance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  `asset` varchar(16) NOT NULL COMMENT '资产币种',
  `available` decimal(32,16) NOT NULL COMMENT '可用余额',
  `frozen` decimal(32,16) NOT NULL COMMENT '冻结余额',
  `update_time` bigint NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_account_asset` (`trade_account`, `asset`),
  KEY `idx_main_account_id` (`main_account_id`),
  KEY `idx_trade_account` (`trade_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='余额表';

CREATE TABLE IF NOT EXISTS `tb_asset_flow` (
  `flow_id` varchar(64) NOT NULL COMMENT '流水ID',
  `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  `asset` varchar(16) NOT NULL COMMENT '资产币种',
  `flow_type` varchar(32) NOT NULL COMMENT '流水类型: DEPOSIT, WITHDRAW, TRADE_IN, TRADE_OUT, FEE',
  `amount` decimal(32,16) NOT NULL COMMENT '金额',
  `balance` decimal(32,16) NOT NULL COMMENT '变更后余额',
  `related_id` varchar(64) DEFAULT NULL COMMENT '关联ID',
  `description` varchar(256) DEFAULT NULL COMMENT '描述',
  `create_time` bigint NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`flow_id`),
  KEY `idx_main_account_id` (`main_account_id`),
  KEY `idx_trade_account` (`trade_account`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产流水表';
