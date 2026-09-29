package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.goods.application.dto.GoodsListItemResponse;
import com.sfood.automation.goods.application.dto.GoodsResponse;
import com.sfood.automation.goods.domain.model.Goods;
import java.time.Instant;

final class GoodsMapper {

    private GoodsMapper() {
    }

    static GoodsResponse toResponse(Goods goods) {
        return new GoodsResponse(
            goods.getId(),
            goods.getGodoGoodsCode(),
            goods.getErpGoodsCode(),
            goods.getGoodsName(),
            goods.getOnlyMallYn(),
            goods.getSoldOutYn(),
            goods.getAvailableQty(),
            goods.getSafetyRate(),
            goods.getRemainValidDays(),
            goods.getGodoSyncYn(),
            goods.getErpSyncYn(),
            goods.getGodoSyncBlockedYn(),
            goods.getDescription(),
            goods.getMessage(),
            goods.getStatus().name(),
            goods.getCreatedId(),
            goods.getCreatedAt(),
            goods.getUpdatedId(),
            goods.getUpdatedAt()
        );
    }

    static GoodsListItemResponse toListItemResponse(Goods goods) {
        return new GoodsListItemResponse(
            goods.getId(),
            goods.getGodoGoodsCode(),
            goods.getErpGoodsCode(),
            goods.getGoodsName(),
            goods.getOnlyMallYn(),
            goods.getSoldOutYn(),
            goods.getAvailableQty(),
            goods.getSafetyRate(),
            goods.getRemainValidDays(),
            goods.getGodoSyncYn(),
            goods.getErpSyncYn(),
            goods.getGodoSyncBlockedYn(),
            goods.getMessage(),
            goods.getStatus().name(),
            recentUpdatedAt(goods),
            recentUpdatedBy(goods)
        );
    }

    private static Instant recentUpdatedAt(Goods goods) {
        return goods.getUpdatedAt() != null ? goods.getUpdatedAt() : goods.getCreatedAt();
    }

    private static String recentUpdatedBy(Goods goods) {
        return goods.getUpdatedId() != null ? goods.getUpdatedId() : goods.getCreatedId();
    }
}
