package com.sfood.automation.goods.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsFixtures;
import com.sfood.automation.goods.domain.model.StatusTransitionResult;
import com.sfood.automation.wms.infrastructure.scraping.AggregatedStock;
import org.junit.jupiter.api.Test;

class GoodsSoldOutPolicyTest {

    private final GoodsSoldOutPolicy policy = new GoodsSoldOutPolicy();

    @Test
    void should_return_skipped_when_zone_is_not_allowed() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), stock(10, 80, true, false, true));

        assertThat(result).isEqualTo(StatusTransitionResult.SKIPPED);
    }

    @Test
    void should_continue_evaluation_when_zone_is_allowed_and_lot_attr_matches() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), stock(21, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.NO_CHANGE);
    }

    @Test
    void should_continue_evaluation_when_b1_zone_is_allowed() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), stock(21, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.NO_CHANGE);
    }

    @Test
    void should_return_skipped_when_lot_attr_is_not_oa110() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), stock(10, 80, true, true, false));

        assertThat(result).isEqualTo(StatusTransitionResult.SKIPPED);
    }

    @Test
    void should_set_out_of_stock_for_general_when_qty_is_20_or_less() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), stock(20, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.SET_OUT_OF_STOCK);
    }

    @Test
    void should_set_out_of_stock_for_on_sale_goods_when_refined_stock_is_missing() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), null);

        assertThat(result).isEqualTo(StatusTransitionResult.SET_OUT_OF_STOCK);
    }

    @Test
    void should_keep_no_change_for_general_when_qty_is_21() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "N"), stock(21, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.NO_CHANGE);
    }

    @Test
    void should_set_out_of_stock_for_exclusive_when_qty_is_1_or_less() {
        StatusTransitionResult result = policy.evaluate(normalGoods("Y", "N"), stock(1, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.SET_OUT_OF_STOCK);
    }

    @Test
    void should_keep_no_change_for_exclusive_when_qty_is_2() {
        StatusTransitionResult result = policy.evaluate(normalGoods("Y", "N"), stock(2, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.NO_CHANGE);
    }

    @Test
    void should_set_on_sale_for_general_when_all_conditions_match() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "Y"), stock(21, 40, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.SET_ON_SALE);
    }

    @Test
    void should_keep_no_change_for_general_when_qty_is_equal_to_goods_available_qty() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "Y"), stock(20, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.NO_CHANGE);
    }

    @Test
    void should_keep_no_change_for_general_when_lot_does_not_exist() {
        StatusTransitionResult result = policy.evaluate(normalGoods("N", "Y"), stock(21, 50, false, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.NO_CHANGE);
    }

    @Test
    void should_set_on_sale_for_exclusive_when_all_conditions_match() {
        StatusTransitionResult result = policy.evaluate(normalGoods("Y", "Y"), stock(2, 50, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.SET_ON_SALE);
    }

    @Test
    void should_be_idempotent_for_same_input() {
        Goods goods = normalGoods("N", "Y");
        AggregatedStock stock = stock(20, 40, true, true, true);

        assertThat(policy.evaluate(goods, stock)).isEqualTo(policy.evaluate(goods, stock));
    }

    @Test
    void should_return_skipped_for_error_goods() {
        Goods goods = normalGoods("N", "N");
        goods.markErpError("ERP sync error");

        StatusTransitionResult result = policy.evaluate(goods, stock(10, 80, true, true, true));

        assertThat(result).isEqualTo(StatusTransitionResult.SKIPPED);
    }

    private Goods normalGoods(String onlyMallYn, String soldOutYn) {
        Goods source = GoodsFixtures.normalGoods();
        return new Goods(
            source.getId(),
            source.getGodoGoodsCode(),
            source.getErpGoodsCode(),
            source.getGoodsName(),
            onlyMallYn,
            soldOutYn,
            "Y".equals(onlyMallYn) ? 1 : source.getAvailableQty(),
            "Y".equals(onlyMallYn) ? 50 : 40,
            source.getRemainValidDays(),
            source.getGodoSyncYn(),
            source.getErpSyncYn(),
            source.getDescription(),
            source.getMessage(),
            GoodsStatus.NORMAL,
            source.getCreatedId(),
            source.getCreatedAt(),
            source.getUpdatedId(),
            source.getUpdatedAt()
        );
    }

    private AggregatedStock stock(
        int qty,
        Integer validPercent,
        boolean lotExists,
        boolean allowedZone,
        boolean oa110Lot
    ) {
        return new AggregatedStock("ERP-0001", qty, validPercent, lotExists, allowedZone, oa110Lot);
    }
}
