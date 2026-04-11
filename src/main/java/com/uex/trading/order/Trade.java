package com.uex.trading.order;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_trade", indexes = {
    @Index(name = "idx_order_id", columnList = "orderId"),
    @Index(name = "idx_main_account_id", columnList = "main_account_id"),
    @Index(name = "idx_trade_account", columnList = "trade_account"),
    @Index(name = "idx_trade_time", columnList = "tradeTime")
})
public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 64)
    private String tradeId;             // 成交ID

    @Column(length = 64, nullable = false)
    private String orderId;             // 订单ID

    @Column(length = 64)
    private String counterOrderId;      // 对手订单ID

    @Column(name = "main_account_id", length = 16, nullable = false)
    private String mainAccountId;       // 所属主账户

    @Column(name = "trade_account", length = 16, nullable = false)
    private String tradeAccount;        // 交易账号

    @Column(length = 32, nullable = false)
    private String symbol;              // 交易对

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal price;           // 成交价格

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal quantity;        // 成交数量

    @Column(precision = 32, scale = 16)
    private BigDecimal fee;             // 手续费

    @Column(length = 16)
    private String feeAsset;            // 手续费币种

    @Column(nullable = false)
    private Long tradeTime;             // 成交时间

    @Column
    private boolean isMaker;            // 是否为Maker
}
