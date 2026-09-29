package com.sfood.automation.wms.infrastructure.scraping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WmsStockAggregator {

    public Map<String, AggregatedStock> aggregate(List<WmsStockItem> items) {
        return items.stream()
            .collect(Collectors.groupingBy(WmsStockItem::itemCd))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<WmsStockItem> grouped = entry.getValue();
                    int totalQty = grouped.stream().mapToInt(WmsStockItem::lalocQty).sum();
                    Integer validPercent = grouped.stream()
                        .map(WmsStockItem::validPercent)
                        .filter(value -> value != null)
                        .max(Integer::compareTo)
                        .orElse(null);
                    Integer remainValidDays = grouped.stream()
                        .map(WmsStockItem::remainValidDays)
                        .filter(value -> value != null)
                        .max(Integer::compareTo)
                        .orElse(null);
                    boolean lotExists = grouped.stream().anyMatch(item -> item.lotNo() != null && !item.lotNo().isBlank());
                    boolean allowedZone = grouped.stream().allMatch(item -> WmsZoneEligibility.isAllowed(item.zoneCd()));
                    boolean oa110Lot = grouped.stream().allMatch(item -> "OA110".equals(item.lotAttr1()));
                    return new AggregatedStock(entry.getKey(), totalQty, validPercent, remainValidDays, lotExists, allowedZone, oa110Lot);
                }
            ));
    }
}
