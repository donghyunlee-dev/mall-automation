package com.sfood.automation.goods.infrastructure.external;

public record GodoParsedResponse(
    String code,
    String message,
    String goodsNo,
    String goodsName,
    String soldOutFl
) {
}
