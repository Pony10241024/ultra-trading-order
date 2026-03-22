package com.uex.trading.order;

import com.uex.trading.common.OrderSide;
import com.uex.trading.common.OrderStatus;
import com.uex.trading.common.OrderType;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tb_order", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_symbol", columnList = "symbol"),
    @Index(name = "idx_create_time", columnList = "createTime")
})
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 64)
    private String orderId;             // 订单ID

    @Column(length = 64, nullable = false)
    private String userId;              // 用户ID

    @Column(length = 32, nullable = false)
    private String symbol;              // 交易对

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private OrderType orderType;        // 订单类型

    @Enumerated(EnumType.STRING)
    @Column(length = 8, nullable = false)
    private OrderSide side;             // 买卖方向

    @Column(precision = 32, scale = 16)
    private BigDecimal price;           // 价格（市价单可为空）

    @Column(precision = 32, scale = 16, nullable = false)
    private BigDecimal quantity;        // 数量

    @Column(precision = 32, scale = 16)
    private BigDecimal filledQty;       // 已成交数量

    @Column(precision = 32, scale = 16)
    private BigDecimal avgPrice;        // 平均成交价格

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private OrderStatus status;         // 订单状态

    @Column(nullable = false)
    private Long createTime;            // 创建时间

    @Column(nullable = false)
    private Long updateTime;            // 更新时间

    @Column(length = 64)
    private String clientOrderId;       // 客户端订单ID
}
