package com.sfood.automation.dashboard.domain.model;

import java.util.List;

public record GodoOrderSearchResult(
    List<GodoOrder> orders,
    List<GodoOrderItem> items,
    boolean hasMore,
    Long nextLastOrder
) {
}
