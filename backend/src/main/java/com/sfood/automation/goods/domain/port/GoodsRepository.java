package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.model.Goods;
import java.util.List;
import java.util.Optional;

public interface GoodsRepository {
    Goods save(Goods goods);
    Optional<Goods> findById(Integer id);
    Optional<Goods> findByGodoCode(String godoGoodsCode);
    Optional<Goods> findByErpCode(String erpGoodsCode);
    List<Goods> findAll(String soldOutYn, String onlyMallYn, String status, String keyword);
    List<Goods> findAllByOnlyMallYn(String onlyMallYn);
    List<Goods> findAllErpSyncNGoods();
    void delete(Goods goods);
}
