package com.sfood.automation.goods.infrastructure.external;

import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.GodoGoodsInfo;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import org.springframework.stereotype.Component;

@Component
public class GodoApiStub implements GodoApiPort {

    @Override
    public GodoGoodsInfo searchGoods(String godoGoodsCode) {
        return new GodoGoodsInfo(godoGoodsCode, "Unverified Goods", "N");
    }

    @Override
    public GodoApiResult updateSoldOutStatus(String godoGoodsCode, String soldOutFl) {
        return new GodoApiResult(true, "000", "success");
    }
}
