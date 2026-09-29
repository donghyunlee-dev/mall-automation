package com.sfood.automation.wms.infrastructure.scraping;

import com.sfood.automation.goods.domain.model.Goods;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class StockDataRefiner {

    private final WmsStockAggregator wmsStockAggregator;

    public StockDataRefiner(WmsStockAggregator wmsStockAggregator) {
        this.wmsStockAggregator = wmsStockAggregator;
    }

    public Map<String, AggregatedStock> refine(List<WmsStockItem> stockItems, Set<String> allErpCodes) {
        List<WmsStockItem> registeredItems = stockItems.stream()
            .filter(item -> allErpCodes.contains(item.itemCd()))
            .filter(this::hasEligibleZone)
            .filter(this::isOa110Lot)
            .toList();
        return wmsStockAggregator.aggregate(registeredItems);
    }

    public Map<String, AggregatedStock> refine(List<WmsStockItem> stockItems, Map<String, Goods> goodsByErpCode) {
        List<WmsStockItem> filteredItems = stockItems.stream()
            .filter(this::hasEligibleZone)
            .filter(this::isOa110Lot)
            .filter(item -> isCriteriaCandidate(item, goodsByErpCode.get(item.itemCd())))
            .toList();
        return wmsStockAggregator.aggregate(filteredItems);
    }

    public Map<String, AggregatedStock> refineForOnSale(List<WmsStockItem> stockItems, Map<String, Goods> goodsByErpCode) {
        List<WmsStockItem> filteredItems = stockItems.stream()
            .filter(this::hasEligibleZone)
            .filter(this::isOa110Lot)
            .filter(item -> isCriteriaCandidate(item, goodsByErpCode.get(item.itemCd())))
            .toList();
        return wmsStockAggregator.aggregate(filteredItems);
    }

    public Map<Integer, AggregatedStock> refineByGoods(List<WmsStockItem> stockItems, List<Goods> goodsList) {
        return refineByGoodsInternal(stockItems, goodsList);
    }

    public Map<Integer, AggregatedStock> refineForOnSaleByGoods(List<WmsStockItem> stockItems, List<Goods> goodsList) {
        return refineByGoodsInternal(stockItems, goodsList);
    }

    private Map<Integer, AggregatedStock> refineByGoodsInternal(List<WmsStockItem> stockItems, List<Goods> goodsList) {
        Map<String, List<WmsStockItem>> stockItemsByErpCode = stockItems.stream()
            .filter(this::hasEligibleZone)
            .filter(this::isOa110Lot)
            .collect(Collectors.groupingBy(WmsStockItem::itemCd));

        Map<Integer, AggregatedStock> refinedStocksByGoodsId = new java.util.LinkedHashMap<>();
        for (Goods goods : goodsList) {
            if (goods.getId() == null) {
                continue;
            }
            List<WmsStockItem> matchedItems = stockItemsByErpCode.get(goods.getErpGoodsCode());
            if (matchedItems == null || matchedItems.isEmpty()) {
                continue;
            }
            List<WmsStockItem> filteredItems = new ArrayList<>();
            for (WmsStockItem item : matchedItems) {
                if (isCriteriaCandidate(item, goods)) {
                    filteredItems.add(item);
                }
            }
            if (filteredItems.isEmpty()) {
                refinedStocksByGoodsId.put(goods.getId(), aggregateWithoutEligibleQuantity(goods.getErpGoodsCode(), matchedItems));
                continue;
            }
            AggregatedStock aggregatedStock = wmsStockAggregator.aggregate(filteredItems).get(goods.getErpGoodsCode());
            if (aggregatedStock != null) {
                refinedStocksByGoodsId.put(goods.getId(), aggregatedStock);
            }
        }
        return refinedStocksByGoodsId;
    }

    private boolean isCriteriaCandidate(WmsStockItem item, Goods goods) {
        if (goods == null) {
            return false;
        }
        if (isFreezeItem(item)) {
            return item.remainValidDays() != null
                && item.remainValidDays() >= goods.getRemainValidDays();
        }
        return item.validPercent() != null
            && item.validPercent() >= goods.getSafetyRate();
    }

    private AggregatedStock aggregateWithoutEligibleQuantity(String erpGoodsCode, List<WmsStockItem> matchedItems) {
        Integer validPercent = matchedItems.stream()
            .map(WmsStockItem::validPercent)
            .filter(value -> value != null)
            .max(Integer::compareTo)
            .orElse(null);
        Integer remainValidDays = matchedItems.stream()
            .map(WmsStockItem::remainValidDays)
            .filter(value -> value != null)
            .max(Integer::compareTo)
            .orElse(null);
        boolean lotExists = matchedItems.stream().anyMatch(item -> item.lotNo() != null && !item.lotNo().isBlank());
        boolean oa110Lot = matchedItems.stream().allMatch(item -> "OA110".equals(item.lotAttr1()));
        return new AggregatedStock(erpGoodsCode, 0, validPercent, remainValidDays, lotExists, true, oa110Lot);
    }

    private boolean isFreezeItem(WmsStockItem item) {
        return "FREEZE".equals(item.tempTcd());
    }

    private boolean hasEligibleZone(WmsStockItem item) {
        return WmsZoneEligibility.isAllowed(item.zoneCd());
    }

    private boolean isOa110Lot(WmsStockItem item) {
        return "OA110".equals(item.lotAttr1());
    }
}
