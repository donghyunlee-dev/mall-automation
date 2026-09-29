package com.sfood.automation.dashboard.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record DashboardSummary(
    BigDecimal revenue,
    long orderCount,
    long buyerCount,
    double avgSku,
    double avgQty
) {
    private static final int SCALE = 2;

    public static DashboardSummary empty() {
        return new DashboardSummary(BigDecimal.ZERO, 0, 0, 0, 0);
    }

    public double orderFrequency() {
        return buyerCount == 0 ? 0 : divide(BigDecimal.valueOf(orderCount), BigDecimal.valueOf(buyerCount));
    }

    public BigDecimal aov() {
        return orderCount == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orderCount), SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal arppu() {
        return buyerCount == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(buyerCount), SCALE, RoundingMode.HALF_UP);
    }

    private double divide(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
