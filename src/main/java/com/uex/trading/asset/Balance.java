package com.uex.trading.asset;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_balance", indexes = {
    @Index(name = "idx_main_account_id", columnList = "main_account_id"),
    @Index(name = "idx_trade_account", columnList = "trade_account")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_trade_account_asset", columnNames = {"trade_account", "asset"})
})
public class Balance implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "main_account_id", length = 16, nullable = false)
    private String mainAccountId;       // 所属主账户

    @Column(name = "trade_account", length = 16, nullable = false)
    private String tradeAccount;        // 交易账号

    @Column(length = 16, nullable = false)
    private String asset;               // 资产币种

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal available;       // 可用余额

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal frozen;          // 冻结余额

    @Column(nullable = false)
    private Long updateTime;            // 更新时间

    @Transient
    public BigDecimal getTotal() {
        return available.add(frozen);
    }
}
