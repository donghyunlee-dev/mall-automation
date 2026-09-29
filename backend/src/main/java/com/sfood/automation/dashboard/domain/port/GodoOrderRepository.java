package com.sfood.automation.dashboard.domain.port;

import com.sfood.automation.dashboard.domain.model.GodoOrder;
import java.util.Optional;

public interface GodoOrderRepository {
    GodoOrder upsert(GodoOrder order);
    Optional<GodoOrder> findByOrderNo(long orderNo);
}
