package com.sfood.automation.goods.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateGoodsRequest(
    @NotBlank String goodsName,
    @NotBlank String erpGoodsCode,
    @NotBlank @Pattern(regexp = "Y|N") String onlyMallYn,
    @NotNull @PositiveOrZero Integer availableQty,
    @NotNull @PositiveOrZero Integer safetyRate,
    @NotNull @PositiveOrZero Integer remainValidDays
) {
}
