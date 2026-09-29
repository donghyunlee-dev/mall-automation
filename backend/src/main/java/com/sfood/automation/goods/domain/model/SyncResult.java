package com.sfood.automation.goods.domain.model;

import java.util.List;

public record SyncResult(
    int outOfStockCount,
    int onSaleCount,
    List<GoodsStatusLog> changedLogs
) {
}
