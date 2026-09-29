package com.sfood.automation.goods.application.dto;

public record CommonStatusConfigResponse(
    String scope,
    int outOfStockQty,
    int onSaleQty,
    int safetyRate,
    int remainValidDays
) {
}
