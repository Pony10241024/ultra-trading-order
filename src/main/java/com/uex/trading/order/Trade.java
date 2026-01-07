package com.uex.trading.order;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tradeId;             // 成交ID
    private String orderId;             // 订单ID
    private String counterOrderId;      // 对手订单ID
    private String userId;              // 用户ID
    private String symbol;              // 交易对
    private BigDecimal price;           // 成交价格
    private BigDecimal quantity;        // 成交数量
    private BigDecimal fee;             // 手续费
    private String feeAsset;            // 手续费币种
    private Long tradeTime;             // 成交时间
    private boolean isMaker;            // 是否为Maker
}
