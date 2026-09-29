package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.model.GodoApiResult;

public interface SfoodAdminGoodsPort {
    GodoApiResult restoreToOnSale(String godoGoodsCode);
}
