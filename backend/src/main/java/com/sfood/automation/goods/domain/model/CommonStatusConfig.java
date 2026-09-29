package com.sfood.automation.goods.domain.model;

import com.sfood.automation.goods.domain.enums.GoodsScope;
import java.time.Instant;

public record CommonStatusConfig(
    Integer id,
    GoodsScope scope,
    int outOfStockQty,
    int onSaleQty,
    int safetyRate,
    int remainValidDays,
    String updatedId,
    Instant updatedAt
) {
}
