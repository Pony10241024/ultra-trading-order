package com.uex.trading.common;

public enum OrderStatus {
    PENDING,        // 待提交
    SUBMITTED,      // 已提交
    PARTIAL_FILLED, // 部分成交
    FILLED,         // 完全成交
    CANCELED,       // 已撤销
    REJECTED        // 已拒绝
}
