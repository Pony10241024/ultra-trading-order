package com.uex.trading.symbol;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SymbolInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;              // 交易对 eg: BTCUSDT
    private String baseAsset;           // 基础货币 eg: BTC
    private String quoteAsset;          // 计价货币 eg: USDT
    private BigDecimal minOrderAmount;  // 最小下单金额
    private BigDecimal minOrderQty;     // 最小下单数量
    private BigDecimal tickSize;        // 价格步长
    private BigDecimal stepSize;        // 数量步长
    private BigDecimal makerFee;        // Maker手续费率
    private BigDecimal takerFee;        // Taker手续费率
    private String exchange;            // 交易所 BINANCE/OKX
    private Long updateTime;            // 更新时间
}
