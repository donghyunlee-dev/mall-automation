package com.sfood.automation.goods.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterGoodsRequest(
    @NotBlank String godoGoodsCode,
    @NotBlank String erpGoodsCode,
    @NotBlank String goodsName,
    @NotBlank @Pattern(regexp = "Y|N") String onlyMallYn
) {
}
