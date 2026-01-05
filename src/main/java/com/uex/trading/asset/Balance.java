package com.uex.trading.asset;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class Balance implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;              // 用户ID
    private String asset;               // 资产币种
    private BigDecimal available;       // 可用余额
    private BigDecimal frozen;          // 冻结余额
    private Long updateTime;            // 更新时间

    public BigDecimal getTotal() {
        return available.add(frozen);
    }
}
