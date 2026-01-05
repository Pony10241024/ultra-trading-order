package com.uex.trading.order;

import com.uex.trading.common.OrderSide;
import com.uex.trading.common.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {
    @NotBlank(message = "Symbol cannot be blank")
    private String symbol;

    @NotNull(message = "Order type cannot be null")
    private OrderType orderType;

    @NotNull(message = "Side cannot be null")
    private OrderSide side;

    private BigDecimal price;       // 限价单必填

    @NotNull(message = "Quantity cannot be null")
    private BigDecimal quantity;

    private String clientOrderId;   // 客户端订单ID（可选）
}
