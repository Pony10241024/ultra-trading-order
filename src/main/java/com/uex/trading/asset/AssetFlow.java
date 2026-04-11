package com.uex.trading.asset;

import com.uex.trading.common.FlowType;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_asset_flow", indexes = {
    @Index(name = "idx_main_account_id", columnList = "main_account_id"),
    @Index(name = "idx_trade_account", columnList = "trade_account"),
    @Index(name = "idx_create_time", columnList = "createTime")
})
public class AssetFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 64)
    private String flowId;              // 流水ID

    @Column(name = "main_account_id", length = 16, nullable = false)
    private String mainAccountId;       // 所属主账户

    @Column(name = "trade_account", length = 16, nullable = false)
    private String tradeAccount;        // 交易账号

    @Column(length = 16, nullable = false)
    private String asset;               // 资产币种

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private FlowType flowType;          // 流水类型

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal amount;          // 金额

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal balance;         // 变更后余额

    @Column(length = 64)
    private String relatedId;           // 关联ID（订单ID或交易ID）

    @Column(length = 256)
    private String description;         // 描述

    @Column(nullable = false)
    private Long createTime;            // 创建时间
}
