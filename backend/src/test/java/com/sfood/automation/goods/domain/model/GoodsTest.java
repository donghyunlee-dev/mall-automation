package com.sfood.automation.goods.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import org.junit.jupiter.api.Test;

class GoodsTest {

    @Test
    void should_restore_hide_goods_to_normal() {
        Goods goods = GoodsFixture.hiddenGoods();
        goods.restore();
        assertThat(goods.getStatus()).isEqualTo(GoodsStatus.NORMAL);
    }

    @Test
    void should_hide_normal_goods() {
        Goods goods = GoodsFixture.normalGoods();
        goods.hide();
        assertThat(goods.getStatus()).isEqualTo(GoodsStatus.HIDE);
    }

    @Test
    void should_apply_godo_sync_result() {
        Goods goods = GoodsFixture.normalGoods();
        goods.applyGodoSyncResult("Y");
        assertThat(goods.getGodoSyncYn()).isEqualTo("Y");
        assertThat(goods.getSoldOutYn()).isEqualTo("Y");
    }

    @Test
    void should_change_scope_and_refresh_criteria() {
        Goods goods = GoodsFixture.normalGoods();
        goods.changeScope("Y", 1, 50, 150);
        assertThat(goods.getOnlyMallYn()).isEqualTo("Y");
        assertThat(goods.getAvailableQty()).isEqualTo(1);
        assertThat(goods.getSafetyRate()).isEqualTo(50);
        assertThat(goods.getRemainValidDays()).isEqualTo(150);
    }

    @Test
    void should_mark_erp_error() {
        Goods goods = GoodsFixture.normalGoods();
        goods.markErpError("sync error");
        assertThat(goods.getStatus()).isEqualTo(GoodsStatus.ERROR);
        assertThat(goods.getErpSyncYn()).isEqualTo("N");
        assertThat(goods.getMessage()).isEqualTo("sync error");
    }

    @Test
    void should_recover_erp_sync() {
        Goods goods = GoodsFixture.normalGoods();
        goods.markErpError("sync error");
        goods.recoverErpSync();
        assertThat(goods.getStatus()).isEqualTo(GoodsStatus.NORMAL);
        assertThat(goods.getErpSyncYn()).isEqualTo("Y");
        assertThat(goods.getMessage()).isNull();
    }
}
