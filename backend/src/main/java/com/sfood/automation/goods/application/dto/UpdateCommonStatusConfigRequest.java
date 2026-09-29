package com.sfood.automation.goods.application.dto;

import jakarta.validation.constraints.Min;

public record UpdateCommonStatusConfigRequest(
    @Min(0) int outOfStockQty,
    @Min(0) int onSaleQty,
    @Min(0) int safetyRate,
    @Min(0) int remainValidDays
) {
}
