package com.uex.trading.asset;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_balance", indexes = {
    @Index(name = "idx_user_id", columnList = "userId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_asset", columnNames = {"userId", "asset"})
})
public class Balance implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String userId;              // 用户ID

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
