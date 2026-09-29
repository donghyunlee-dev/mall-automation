package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import java.util.List;

public interface GoodsStatusLogRepository {
    GoodsStatusLog save(GoodsStatusLog goodsStatusLog);
    List<GoodsStatusLog> findAll(String godoGoodsCode, String changedFrom, String changedTo);
}
