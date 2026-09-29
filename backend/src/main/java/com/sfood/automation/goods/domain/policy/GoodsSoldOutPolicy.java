package com.sfood.automation.goods.domain.policy;

import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.StatusTransitionResult;
import com.sfood.automation.wms.infrastructure.scraping.AggregatedStock;
import org.springframework.stereotype.Component;

@Component
public class GoodsSoldOutPolicy {

    public StatusTransitionResult evaluate(Goods goods, AggregatedStock aggregatedStock) {
        if (goods == null) {
            return StatusTransitionResult.NO_CHANGE;
        }
        if (aggregatedStock == null) {
            return "N".equals(goods.getSoldOutYn())
                ? StatusTransitionResult.SET_OUT_OF_STOCK
                : StatusTransitionResult.NO_CHANGE;
        }
        if (!aggregatedStock.allowedZone() || !aggregatedStock.oa110Lot()) {
            return StatusTransitionResult.SKIPPED;
        }
        if (!"NORMAL".equals(goods.getStatus().name())) {
            return StatusTransitionResult.SKIPPED;
        }

        if ("N".equals(goods.getSoldOutYn())) {
            return aggregatedStock.totalInvnQty() <= goods.getAvailableQty()
                ? StatusTransitionResult.SET_OUT_OF_STOCK
                : StatusTransitionResult.NO_CHANGE;
        }

        return aggregatedStock.lotExists()
            && aggregatedStock.totalInvnQty() >= goods.getAvailableQty() + 1
            ? StatusTransitionResult.SET_ON_SALE
            : StatusTransitionResult.NO_CHANGE;
    }
}
