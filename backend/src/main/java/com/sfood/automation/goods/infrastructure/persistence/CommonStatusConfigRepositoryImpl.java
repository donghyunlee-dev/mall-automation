package com.sfood.automation.goods.infrastructure.persistence;

import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.CommonStatusConfig;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CommonStatusConfigRepositoryImpl implements CommonStatusConfigRepository {

    private final CommonStatusConfigJpaRepository jpaRepository;

    public CommonStatusConfigRepositoryImpl(CommonStatusConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<CommonStatusConfig> findAll() {
        return jpaRepository.findAllByOrderByIdAsc().stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<CommonStatusConfig> findByScope(GoodsScope scope) {
        return jpaRepository.findByScope(scope.name()).map(this::toDomain);
    }

    @Override
    public CommonStatusConfig save(CommonStatusConfig config) {
        CommonStatusConfigEntity entity = new CommonStatusConfigEntity();
        entity.setId(config.id());
        entity.setScope(config.scope().name());
        entity.setOutOfStockQty(config.outOfStockQty());
        entity.setOnSaleQty(config.onSaleQty());
        entity.setSafetyRate(config.safetyRate());
        entity.setRemainValidDays(config.remainValidDays());
        entity.setUpdatedId(config.updatedId());
        entity.setUpdatedAt(config.updatedAt().toString());
        return toDomain(jpaRepository.save(entity));
    }

    private CommonStatusConfig toDomain(CommonStatusConfigEntity entity) {
        return new CommonStatusConfig(
            entity.getId(),
            GoodsScope.valueOf(entity.getScope()),
            entity.getOutOfStockQty(),
            entity.getOnSaleQty(),
            entity.getSafetyRate(),
            entity.getRemainValidDays(),
            entity.getUpdatedId(),
            SqliteInstantParser.parse(entity.getUpdatedAt())
        );
    }
}
