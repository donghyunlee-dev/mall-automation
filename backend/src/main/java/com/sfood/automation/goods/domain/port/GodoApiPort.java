package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.GodoGoodsInfo;

public interface GodoApiPort {
    GodoGoodsInfo searchGoods(String godoGoodsCode);
    GodoApiResult updateSoldOutStatus(String godoGoodsCode, String soldOutFl);
}
