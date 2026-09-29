package com.sfood.automation.goods.domain.model;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import java.time.Instant;

final class GoodsFixture {

    private GoodsFixture() {
    }

    static Goods normalGoods() {
        return new Goods(
            1,
            "10000001",
            "ERP-0001",
            "Sample Goods",
            "N",
            "N",
            20,
            50,
            150,
            "N",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.now(),
            null,
            null
        );
    }

    static Goods hiddenGoods() {
        return new Goods(
            1,
            "10000001",
            "ERP-0001",
            "Sample Goods",
            "N",
            "N",
            20,
            50,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.HIDE,
            "user@s-food.com",
            Instant.now(),
            null,
            null
        );
    }
}
