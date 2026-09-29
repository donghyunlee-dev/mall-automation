package com.sfood.automation.goods.application.dto;

import java.time.Instant;

public record GoodsResponse(
    Integer id,
    String godoGoodsCode,
    String erpGoodsCode,
    String goodsName,
    String onlyMallYn,
    String soldOutYn,
    int availableQty,
    int safetyRate,
    int remainValidDays,
    String godoSyncYn,
    String erpSyncYn,
    String godoSyncBlockedYn,
    String description,
    String message,
    String status,
    String createdId,
    Instant createdAt,
    String updatedId,
    Instant updatedAt
) {
}
