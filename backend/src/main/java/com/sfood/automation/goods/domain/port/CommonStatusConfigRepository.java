package com.sfood.automation.goods.domain.port;

import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.CommonStatusConfig;
import java.util.List;
import java.util.Optional;

public interface CommonStatusConfigRepository {
    List<CommonStatusConfig> findAll();
    Optional<CommonStatusConfig> findByScope(GoodsScope scope);
    CommonStatusConfig save(CommonStatusConfig config);
}
