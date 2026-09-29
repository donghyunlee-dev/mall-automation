package com.sfood.automation.wms.infrastructure.scraping;

import com.sfood.automation.goods.domain.model.Goods;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ErpCodeVerifier {

    public List<ErpVerifyResult> verify(List<WmsStockItem> stockItems, List<Goods> erpSyncNGoods, List<Goods> alreadySyncedGoods) {
        Set<String> stockItemCodes = stockItems.stream().map(WmsStockItem::itemCd).collect(java.util.stream.Collectors.toSet());
        Set<Integer> alreadySyncedIds = alreadySyncedGoods.stream().map(Goods::getId).collect(java.util.stream.Collectors.toSet());

        return erpSyncNGoods.stream()
            .filter(goods -> !alreadySyncedIds.contains(goods.getId()))
            .map(goods -> stockItemCodes.contains(goods.getErpGoodsCode())
                ? new ErpVerifyResult(goods.getId(), "Y", "NORMAL", null)
                : new ErpVerifyResult(goods.getId(), "N", "ERROR", "ERP goods code not found in WMS stock"))
            .toList();
    }
}
