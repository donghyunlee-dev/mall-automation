package com.sfood.automation.dashboard.domain.model;

import java.math.BigDecimal;

public record GodoOrder(
    Integer id,
    long orderNo,
    String memberId,
    String memberGrade,
    String orderStatus,
    String orderedAt,
    BigDecimal totalGoodsPrice,
    BigDecimal totalDeliveryCharge,
    BigDecimal useDeposit,
    BigDecimal useMileage,
    BigDecimal totalGoodsDcPrice,
    BigDecimal totalMemberDcPrice,
    BigDecimal totalCouponDcPrice,
    BigDecimal paymentFinalAmount,
    String createdAt,
    String updatedAt
) {
}
