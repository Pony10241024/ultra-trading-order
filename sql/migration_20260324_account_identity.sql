-- Existing database migration:
-- Replace legacy user_id with main_account_id + trade_account
-- Execute only for old environments that still use user_id columns.
-- Backup the database before running.

SET NAMES utf8mb4;

ALTER TABLE `tb_order`
  ADD COLUMN `main_account_id` varchar(16) DEFAULT NULL COMMENT '所属主账户（关联client_main_account）' AFTER `order_id`,
  ADD COLUMN `trade_account` varchar(16) DEFAULT NULL COMMENT '交易账号（唯一标识）' AFTER `main_account_id`;

UPDATE `tb_order`
SET `main_account_id` = LEFT(`user_id`, 16),
    `trade_account` = LEFT(`user_id`, 16)
WHERE `main_account_id` IS NULL
   OR `trade_account` IS NULL;

ALTER TABLE `tb_order`
  MODIFY COLUMN `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  MODIFY COLUMN `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  ADD KEY `idx_main_account_id` (`main_account_id`),
  ADD KEY `idx_trade_account` (`trade_account`),
  DROP COLUMN `user_id`;

ALTER TABLE `tb_trade`
  ADD COLUMN `main_account_id` varchar(16) DEFAULT NULL COMMENT '所属主账户（关联client_main_account）' AFTER `counter_order_id`,
  ADD COLUMN `trade_account` varchar(16) DEFAULT NULL COMMENT '交易账号（唯一标识）' AFTER `main_account_id`;

UPDATE `tb_trade`
SET `main_account_id` = LEFT(`user_id`, 16),
    `trade_account` = LEFT(`user_id`, 16)
WHERE `main_account_id` IS NULL
   OR `trade_account` IS NULL;

ALTER TABLE `tb_trade`
  MODIFY COLUMN `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  MODIFY COLUMN `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  ADD KEY `idx_main_account_id` (`main_account_id`),
  ADD KEY `idx_trade_account` (`trade_account`),
  DROP COLUMN `user_id`;

ALTER TABLE `tb_balance`
  ADD COLUMN `main_account_id` varchar(16) DEFAULT NULL COMMENT '所属主账户（关联client_main_account）' AFTER `id`,
  ADD COLUMN `trade_account` varchar(16) DEFAULT NULL COMMENT '交易账号（唯一标识）' AFTER `main_account_id`;

UPDATE `tb_balance`
SET `main_account_id` = LEFT(`user_id`, 16),
    `trade_account` = LEFT(`user_id`, 16)
WHERE `main_account_id` IS NULL
   OR `trade_account` IS NULL;

ALTER TABLE `tb_balance`
  DROP INDEX `uk_user_id_asset`,
  MODIFY COLUMN `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  MODIFY COLUMN `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  ADD UNIQUE KEY `uk_trade_account_asset` (`trade_account`, `asset`),
  ADD KEY `idx_main_account_id` (`main_account_id`),
  ADD KEY `idx_trade_account` (`trade_account`),
  DROP COLUMN `user_id`;

ALTER TABLE `tb_asset_flow`
  ADD COLUMN `main_account_id` varchar(16) DEFAULT NULL COMMENT '所属主账户（关联client_main_account）' AFTER `flow_id`,
  ADD COLUMN `trade_account` varchar(16) DEFAULT NULL COMMENT '交易账号（唯一标识）' AFTER `main_account_id`;

UPDATE `tb_asset_flow`
SET `main_account_id` = LEFT(`user_id`, 16),
    `trade_account` = LEFT(`user_id`, 16)
WHERE `main_account_id` IS NULL
   OR `trade_account` IS NULL;

ALTER TABLE `tb_asset_flow`
  MODIFY COLUMN `main_account_id` varchar(16) NOT NULL COMMENT '所属主账户（关联client_main_account）',
  MODIFY COLUMN `trade_account` varchar(16) NOT NULL COMMENT '交易账号（唯一标识）',
  ADD KEY `idx_main_account_id` (`main_account_id`),
  ADD KEY `idx_trade_account` (`trade_account`),
  DROP COLUMN `user_id`;
