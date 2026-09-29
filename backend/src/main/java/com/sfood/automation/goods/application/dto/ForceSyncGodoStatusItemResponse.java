package com.sfood.automation.goods.application.dto;

public record ForceSyncGodoStatusItemResponse(
    String godoGoodsCode,
    String result,
    String message,
    String soldOutYn
) {
}
