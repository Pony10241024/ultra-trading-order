package com.uex.trading.asset;

import com.uex.trading.common.FlowType;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AssetFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    private String flowId;              // 流水ID
    private String userId;              // 用户ID
    private String asset;               // 资产币种
    private FlowType flowType;          // 流水类型
    private BigDecimal amount;          // 金额
    private BigDecimal balance;         // 变更后余额
    private String relatedId;           // 关联ID（订单ID或交易ID）
    private String description;         // 描述
    private Long createTime;            // 创建时间
}
