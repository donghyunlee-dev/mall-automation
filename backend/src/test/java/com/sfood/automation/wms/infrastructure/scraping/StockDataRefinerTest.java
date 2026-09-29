package com.sfood.automation.wms.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StockDataRefinerTest {

    private final StockDataRefiner refiner = new StockDataRefiner(new WmsStockAggregator());

    @Test
    void should_remove_unregistered_item_codes() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(item("ERP-0001"), item("ERP-9999")),
            Set.of("ERP-0001")
        );

        assertThat(refined).hasSize(1);
        assertThat(refined).containsOnlyKeys("ERP-0001");
    }

    @Test
    void should_keep_single_item_when_all_item_codes_registered() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(item("ERP-0001"), item("ERP-0002")),
            Set.of("ERP-0001", "ERP-0002")
        );

        assertThat(refined).hasSize(2);
        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(10);
        assertThat(refined.get("ERP-0002").totalInvnQty()).isEqualTo(10);
    }

    @Test
    void should_aggregate_multiple_lots_for_same_item() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(
                item("ERP-0001", 10, 80, "LOT-1"),
                item("ERP-0001", 15, 70, "LOT-2"),
                item("ERP-9999", 99, 60, "LOT-X")
            ),
            Set.of("ERP-0001")
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(25);
        assertThat(refined.get("ERP-0001").minValidPercent()).isEqualTo(80);
        assertThat(refined.get("ERP-0001").lotExists()).isTrue();
    }

    @Test
    void should_filter_on_sale_candidates_by_goods_safety_rate_before_aggregation() {
        Map<String, AggregatedStock> refined = refiner.refineForOnSale(
            List.of(
                item("ERP-0001", 10, 80, "LOT-1"),
                item("ERP-0001", 15, 35, "LOT-2"),
                item("ERP-9999", 99, 90, "LOT-X")
            ),
            Map.of("ERP-0001", goods("ERP-0001", 20, 40))
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(10);
        assertThat(refined.get("ERP-0001").minValidPercent()).isEqualTo(80);
    }

    @Test
    void should_filter_sold_out_candidates_by_goods_safety_rate_before_aggregation() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(
                item("ERP-0001", 10, 80, "LOT-1"),
                item("ERP-0001", 15, 35, "LOT-2"),
                item("ERP-9999", 99, 90, "LOT-X")
            ),
            Map.of("ERP-0001", goods("ERP-0001", 20, 40))
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(10);
        assertThat(refined.get("ERP-0001").minValidPercent()).isEqualTo(80);
    }

    @Test
    void should_exclude_rows_with_missing_valid_percent_from_on_sale_candidates() {
        Map<String, AggregatedStock> refined = refiner.refineForOnSale(
            List.of(
                new WmsStockItem("ERP-0001", "Sample", 10, null, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"),
                item("ERP-0001", 5, 60, "LOT-2")
            ),
            Map.of("ERP-0001", goods("ERP-0001", 20, 50))
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(5);
        assertThat(refined.get("ERP-0001").minValidPercent()).isEqualTo(60);
    }

    @Test
    void should_exclude_rows_with_missing_valid_percent_from_sold_out_candidates() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(
                new WmsStockItem("ERP-0001", "Sample", 10, null, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"),
                item("ERP-0001", 5, 60, "LOT-2")
            ),
            Map.of("ERP-0001", goods("ERP-0001", 20, 50))
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(5);
        assertThat(refined.get("ERP-0001").minValidPercent()).isEqualTo(60);
    }

    @Test
    void should_remove_non_allowed_zone_rows_before_aggregating_registered_stock() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(
                item("279110073", 38, 47, "LOT-1", "43"),
                item("279110073", 40, 47, "LOT-1", "SHP")
            ),
            Set.of("279110073")
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("279110073").totalInvnQty()).isEqualTo(38);
        assertThat(refined.get("279110073").allowedZone()).isTrue();
    }

    @Test
    void should_remove_non_allowed_zone_rows_before_filtering_on_sale_candidates() {
        Map<String, AggregatedStock> refined = refiner.refineForOnSale(
            List.of(
                item("279110073", 38, 47, "LOT-1", "43"),
                item("279110073", 40, 47, "LOT-1", "SHP")
            ),
            Map.of("279110073", goods("279110073", 20, 40))
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("279110073").totalInvnQty()).isEqualTo(38);
        assertThat(refined.get("279110073").minValidPercent()).isEqualTo(47);
    }

    @Test
    void should_include_b1_zone_rows_as_allowed_stock() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(
                item("279110073", 30, 47, "LOT-1", "43"),
                item("279110073", 12, 47, "LOT-2", "B1"),
                item("279110073", 8, 47, "LOT-3", "SHP")
            ),
            Set.of("279110073")
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get("279110073").totalInvnQty()).isEqualTo(42);
        assertThat(refined.get("279110073").allowedZone()).isTrue();
    }

    @Test
    void should_exclude_non_oa110_items_from_refineByGoods() {
        Goods goods = goods(1, "10000001", "ERP-0001", 20, 50);

        Map<Integer, AggregatedStock> refined = refiner.refineByGoods(
            List.of(
                itemWithLotAttr("ERP-0001", 30, 80, "LOT-1", "OA110"),
                itemWithLotAttr("ERP-0001", 20, 80, "LOT-2", "OA220")
            ),
            List.of(goods)
        );

        assertThat(refined.get(1).totalInvnQty()).isEqualTo(30);
    }

    @Test
    void should_exclude_non_oa110_items_from_refine_with_erp_codes() {
        Map<String, AggregatedStock> refined = refiner.refine(
            List.of(
                itemWithLotAttr("ERP-0001", 30, 80, "LOT-1", "OA110"),
                itemWithLotAttr("ERP-0001", 20, 80, "LOT-2", "OA220")
            ),
            Set.of("ERP-0001")
        );

        assertThat(refined.get("ERP-0001").totalInvnQty()).isEqualTo(30);
    }

    @Test
    void should_refine_duplicate_erp_goods_per_goods_safety_rate() {
        Goods strictGoods = goods(1, "10000001", "ERP-0001", 20, 50);
        Goods relaxedGoods = goods(2, "10000002", "ERP-0001", 5, 30);

        Map<Integer, AggregatedStock> refined = refiner.refineByGoods(
            List.of(
                item("ERP-0001", 10, 80, "LOT-1"),
                item("ERP-0001", 15, 35, "LOT-2")
            ),
            List.of(strictGoods, relaxedGoods)
        );

        assertThat(refined).hasSize(2);
        assertThat(refined.get(1).totalInvnQty()).isEqualTo(10);
        assertThat(refined.get(1).minValidPercent()).isEqualTo(80);
        assertThat(refined.get(2).totalInvnQty()).isEqualTo(25);
        assertThat(refined.get(2).minValidPercent()).isEqualTo(80);
    }

    @Test
    void should_filter_freeze_rows_by_goods_remain_valid_days_before_aggregation() {
        Goods freezeGoods = goods(1, "10000001", "ERP-0001", 20, 50, 150);

        Map<Integer, AggregatedStock> refined = refiner.refineByGoods(
            List.of(
                freezeItem("ERP-0001", 10, 160, "LOT-1"),
                freezeItem("ERP-0001", 15, 140, "LOT-2")
            ),
            List.of(freezeGoods)
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get(1).totalInvnQty()).isEqualTo(10);
        assertThat(refined.get(1).remainValidDays()).isEqualTo(160);
    }

    @Test
    void should_keep_max_failing_valid_percent_when_no_non_freeze_rows_meet_threshold() {
        Goods goods = goods(1, "10000001", "ERP-0001", 20, 50, 150);

        Map<Integer, AggregatedStock> refined = refiner.refineByGoods(
            List.of(
                item("ERP-0001", 10, 44, "LOT-1"),
                item("ERP-0001", 15, 49, "LOT-2")
            ),
            List.of(goods)
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get(1).totalInvnQty()).isZero();
        assertThat(refined.get(1).validPercent()).isEqualTo(49);
        assertThat(refined.get(1).remainValidDays()).isEqualTo(149);
    }

    @Test
    void should_keep_max_failing_remain_valid_days_when_no_freeze_rows_meet_threshold() {
        Goods goods = goods(1, "10000001", "ERP-0001", 20, 50, 150);

        Map<Integer, AggregatedStock> refined = refiner.refineByGoods(
            List.of(
                freezeItem("ERP-0001", 10, 140, "LOT-1"),
                freezeItem("ERP-0001", 15, 149, "LOT-2")
            ),
            List.of(goods)
        );

        assertThat(refined).hasSize(1);
        assertThat(refined.get(1).totalInvnQty()).isZero();
        assertThat(refined.get(1).validPercent()).isEqualTo(80);
        assertThat(refined.get(1).remainValidDays()).isEqualTo(149);
    }

    private WmsStockItem item(String itemCd) {
        return item(itemCd, 10, 80, "LOT-1");
    }

    private WmsStockItem item(String itemCd, int qty, int validPercent, String lotNo) {
        return item(itemCd, qty, validPercent, 149, lotNo, "104");
    }

    private WmsStockItem item(String itemCd, int qty, int validPercent, String lotNo, String zoneCd) {
        return item(itemCd, qty, validPercent, 149, lotNo, zoneCd);
    }

    private WmsStockItem item(String itemCd, int qty, int validPercent, int remainValidDays, String lotNo, String zoneCd) {
        return new WmsStockItem(itemCd, "Sample", qty, validPercent, remainValidDays, lotNo, "OA110", zoneCd, "COOL", "", "SLP001", "2026-04-01T00:00:00");
    }

    private WmsStockItem itemWithLotAttr(String itemCd, int qty, int validPercent, String lotNo, String lotAttr1) {
        return new WmsStockItem(itemCd, "Sample", qty, validPercent, 149, lotNo, lotAttr1, "104", "COOL", "", "SLP001", "2026-04-01T00:00:00");
    }

    private WmsStockItem freezeItem(String itemCd, int qty, int remainValidDays, String lotNo) {
        return freezeItem(itemCd, qty, 80, remainValidDays, lotNo);
    }

    private WmsStockItem freezeItem(String itemCd, int qty, int validPercent, int remainValidDays, String lotNo) {
        return new WmsStockItem(itemCd, "Sample", qty, validPercent, remainValidDays, lotNo, "OA110", "104", "FREEZE", "", "SLP001", "2026-04-01T00:00:00");
    }

    private Goods goods(String erpCode, int availableQty, int safetyRate) {
        return goods(1, "10000001", erpCode, availableQty, safetyRate, 150);
    }

    private Goods goods(int id, String godoCode, String erpCode, int availableQty, int safetyRate) {
        return goods(id, godoCode, erpCode, availableQty, safetyRate, 150);
    }

    private Goods goods(int id, String godoCode, String erpCode, int availableQty, int safetyRate, int remainValidDays) {
        return new Goods(
            id,
            godoCode,
            erpCode,
            "Sample Goods",
            "N",
            "N",
            availableQty,
            safetyRate,
            remainValidDays,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            java.time.Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );
    }
}
