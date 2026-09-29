package com.sfood.automation.dashboard.domain.port;

import com.sfood.automation.dashboard.domain.model.GodoOrderItem;
import java.util.List;

public interface GodoOrderItemRepository {
    GodoOrderItem upsert(GodoOrderItem item);
    List<GodoOrderItem> findByOrderNo(long orderNo);
}
