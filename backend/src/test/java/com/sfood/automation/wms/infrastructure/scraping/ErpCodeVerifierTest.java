package com.sfood.automation.wms.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsFixtures;
import com.sfood.automation.goods.domain.enums.GoodsStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErpCodeVerifierTest {

    private final ErpCodeVerifier verifier = new ErpCodeVerifier();

    @Test
    void should_skip_goods_already_synced() {
        Goods syncedGoods = GoodsFixtures.normalGoods();

        List<ErpVerifyResult> results = verifier.verify(List.of(item("ERP-0001")), List.of(), List.of(syncedGoods));

        assertThat(results).isEmpty();
    }

    @Test
    void should_mark_normal_when_item_code_exists() {
        Goods goods = unsyncedGoods();

        List<ErpVerifyResult> results = verifier.verify(List.of(item(goods.getErpGoodsCode())), List.of(goods), List.of());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).erpSyncYn()).isEqualTo("Y");
        assertThat(results.get(0).status()).isEqualTo("NORMAL");
    }

    @Test
    void should_mark_error_when_item_code_missing() {
        Goods goods = unsyncedGoods();

        List<ErpVerifyResult> results = verifier.verify(List.of(item("ERP-OTHER")), List.of(goods), List.of());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).erpSyncYn()).isEqualTo("N");
        assertThat(results.get(0).status()).isEqualTo("ERROR");
        assertThat(results.get(0).message()).isNotBlank();
    }

    @Test
    void should_return_empty_when_no_goods_to_verify() {
        List<ErpVerifyResult> results = verifier.verify(List.of(item("ERP-0001")), List.of(), List.of());

        assertThat(results).isEmpty();
    }

    private WmsStockItem item(String itemCd) {
        return new WmsStockItem(itemCd, "Sample", 10, 80, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00");
    }

    private Goods unsyncedGoods() {
        Goods goods = GoodsFixtures.normalGoods();
        return new Goods(
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
            "N",
            goods.getDescription(),
            goods.getMessage(),
            GoodsStatus.NORMAL,
            goods.getCreatedId(),
            goods.getCreatedAt(),
            goods.getUpdatedId(),
            goods.getUpdatedAt()
        );
    }
}
