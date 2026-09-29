package com.sfood.automation.dashboard.domain.model;

import java.math.BigDecimal;

public record GodoOrderItem(
    Integer id,
    String itemType,
    long godoLineNo,
    long orderNo,
    String productName,
    int productQty,
    BigDecimal productUnitPrice,
    BigDecimal productLineRevenue,
    String deliveredAt,
    String omsShippedAt,
    String createdAt,
    String updatedAt
) {
}
