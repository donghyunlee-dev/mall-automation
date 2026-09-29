package com.sfood.automation.goods.application.dto;

import java.time.Instant;

public record GoodsListItemResponse(
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
    String message,
    String status,
    Instant recentUpdatedAt,
    String recentUpdatedBy
) {
}
