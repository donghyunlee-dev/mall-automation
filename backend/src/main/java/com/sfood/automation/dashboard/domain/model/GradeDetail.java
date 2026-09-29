package com.sfood.automation.dashboard.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record GradeDetail(
    String grade,
    BigDecimal revenue,
    long orderCount,
    long buyerCount,
    double avgSku,
    double avgQty
) {
    private static final int SCALE = 2;

    public BigDecimal aov() {
        return orderCount == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orderCount), SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal arppu() {
        return buyerCount == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(buyerCount), SCALE, RoundingMode.HALF_UP);
    }
}
