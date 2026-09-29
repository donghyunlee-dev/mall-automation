package com.sfood.automation.goods.domain.model;

public record GodoGoodsInfo(
    String godoGoodsCode,
    String goodsName,
    String soldOutYn
) {
    public boolean matches(String requestedGodoGoodsCode) {
        return requestedGodoGoodsCode != null && requestedGodoGoodsCode.equals(godoGoodsCode);
    }
}
