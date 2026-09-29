package com.sfood.automation.dashboard.domain.port;

import com.sfood.automation.dashboard.domain.model.GodoOrderSearchQuery;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;

public interface GodoOrderSearchPort {
    GodoOrderSearchResult search(GodoOrderSearchQuery query);
}
