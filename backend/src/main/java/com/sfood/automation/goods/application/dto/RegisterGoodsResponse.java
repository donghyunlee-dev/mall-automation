package com.sfood.automation.goods.application.dto;

public record RegisterGoodsResponse(
    Integer id,
    String godoGoodsCode,
    String erpGoodsCode,
    String status,
    String message
) {
}
