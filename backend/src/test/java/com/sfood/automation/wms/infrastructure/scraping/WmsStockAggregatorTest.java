package com.sfood.automation.wms.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WmsStockAggregatorTest {

    private final WmsStockAggregator aggregator = new WmsStockAggregator();

    @Test
    void should_sum_quantity_for_same_item() {
        Map<String, AggregatedStock> result = aggregator.aggregate(List.of(
            item("ERP-0001", 30, 80, "LOT-1"),
            item("ERP-0001", 20, 70, "LOT-2"),
            item("ERP-0001", 10, 60, "LOT-3")
        ));

        assertThat(result.get("ERP-0001").totalInvnQty()).isEqualTo(60);
        assertThat(result.get("ERP-0001").lotExists()).isTrue();
    }

    @Test
    void should_keep_single_quantity_for_single_lot() {
        Map<String, AggregatedStock> result = aggregator.aggregate(List.of(
            item("ERP-0001", 15, 90, "LOT-1")
        ));

        assertThat(result.get("ERP-0001").totalInvnQty()).isEqualTo(15);
        assertThat(result.get("ERP-0001").lotExists()).isTrue();
    }

    @Test
    void should_select_max_valid_percent() {
        Map<String, AggregatedStock> result = aggregator.aggregate(List.of(
            item("ERP-0001", 30, 80, "LOT-1"),
            item("ERP-0001", 20, 65, "LOT-2"),
            item("ERP-0001", 10, 70, "LOT-3")
        ));

        assertThat(result.get("ERP-0001").minValidPercent()).isEqualTo(80);
    }

    private WmsStockItem item(String itemCd, int qty, int validPercent, String lotNo) {
        return new WmsStockItem(itemCd, "Sample", qty, validPercent, lotNo, "OA110", "104", "", "SLP001", "2026-04-01T00:00:00");
    }
}
