package com.uex.trading.order;

import com.uex.trading.common.OrderSide;
import com.uex.trading.common.OrderStatus;
import com.uex.trading.common.OrderType;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId;             // 订单ID
    private String userId;              // 用户ID
    private String symbol;              // 交易对
    private OrderType orderType;        // 订单类型
    private OrderSide side;             // 买卖方向
    private BigDecimal price;           // 价格（市价单可为空）
    private BigDecimal quantity;        // 数量
    private BigDecimal filledQty;       // 已成交数量
    private BigDecimal avgPrice;        // 平均成交价格
    private OrderStatus status;         // 订单状态
    private Long createTime;            // 创建时间
    private Long updateTime;            // 更新时间
    private String clientOrderId;       // 客户端订单ID
}
