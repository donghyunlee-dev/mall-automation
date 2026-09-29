package com.sfood.automation.dashboard.application.dto;

import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import java.math.BigDecimal;

public record DashboardSummaryResponse(
    BigDecimal revenue,
    long orderCount,
    long buyerCount,
    double orderFrequency,
    double avgSku,
    double avgQty,
    BigDecimal aov,
    BigDecimal arppu
) {
    public static DashboardSummaryResponse from(DashboardSummary summary) {
        return new DashboardSummaryResponse(
            summary.revenue(),
            summary.orderCount(),
            summary.buyerCount(),
            summary.orderFrequency(),
            summary.avgSku(),
            summary.avgQty(),
            summary.aov(),
            summary.arppu()
        );
    }
}
