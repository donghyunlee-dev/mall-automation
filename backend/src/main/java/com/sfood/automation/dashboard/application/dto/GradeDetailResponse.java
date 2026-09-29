package com.sfood.automation.dashboard.application.dto;

import com.sfood.automation.dashboard.domain.model.GradeDetail;
import java.math.BigDecimal;

public record GradeDetailResponse(
    String grade,
    BigDecimal revenue,
    long orderCount,
    long buyerCount,
    double avgSku,
    double avgQty,
    BigDecimal aov,
    BigDecimal arppu
) {
    public static GradeDetailResponse from(GradeDetail detail) {
        return new GradeDetailResponse(
            detail.grade(),
            detail.revenue(),
            detail.orderCount(),
            detail.buyerCount(),
            detail.avgSku(),
            detail.avgQty(),
            detail.aov(),
            detail.arppu()
        );
    }
}
