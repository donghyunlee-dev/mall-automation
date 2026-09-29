package com.sfood.automation.goods.infrastructure.persistence;

import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.port.GoodsStatusLogRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class GoodsStatusLogRepositoryImpl implements GoodsStatusLogRepository {

    private final GoodsStatusLogJpaRepository goodsStatusLogJpaRepository;

    public GoodsStatusLogRepositoryImpl(GoodsStatusLogJpaRepository goodsStatusLogJpaRepository) {
        this.goodsStatusLogJpaRepository = goodsStatusLogJpaRepository;
    }

    @Override
    public GoodsStatusLog save(GoodsStatusLog goodsStatusLog) {
        return toDomain(goodsStatusLogJpaRepository.save(toEntity(goodsStatusLog)));
    }

    @Override
    public List<GoodsStatusLog> findAll(String godoGoodsCode, String changedFrom, String changedTo) {
        return goodsStatusLogJpaRepository.findAll().stream()
            .filter(entity -> godoGoodsCode == null || godoGoodsCode.equals(entity.getGodoGoodsCode()))
            .filter(entity -> changedFrom == null || entity.getChangedAt().compareTo(changedFrom) >= 0)
            .filter(entity -> changedTo == null || entity.getChangedAt().compareTo(changedTo) <= 0)
            .sorted(Comparator.comparing(GoodsStatusLogEntity::getChangedAt).reversed())
            .map(this::toDomain)
            .toList();
    }

    private GoodsStatusLogEntity toEntity(GoodsStatusLog goodsStatusLog) {
        GoodsStatusLogEntity entity = new GoodsStatusLogEntity();
        entity.setId(goodsStatusLog.id());
        entity.setGodoGoodsCode(goodsStatusLog.godoGoodsCode());
        entity.setErpGoodsCode(goodsStatusLog.erpGoodsCode());
        entity.setWmsAvailableQty(goodsStatusLog.wmsAvailableQty());
        entity.setCriteriaQty(goodsStatusLog.criteriaQty());
        entity.setSafetyRate(goodsStatusLog.safetyRate());
        entity.setValidPercent(goodsStatusLog.validPercent());
        entity.setRemainValidDays(goodsStatusLog.remainValidDays());
        entity.setBeforeStatus(goodsStatusLog.beforeStatus());
        entity.setAfterStatus(goodsStatusLog.afterStatus());
        entity.setChangedAt(goodsStatusLog.changedAt());
        entity.setCreatedAt(goodsStatusLog.createdAt());
        return entity;
    }

    private GoodsStatusLog toDomain(GoodsStatusLogEntity entity) {
        return new GoodsStatusLog(
            entity.getId(),
            entity.getGodoGoodsCode(),
            entity.getErpGoodsCode(),
            entity.getWmsAvailableQty(),
            entity.getCriteriaQty(),
            entity.getSafetyRate(),
            entity.getValidPercent(),
            entity.getRemainValidDays(),
            entity.getBeforeStatus(),
            entity.getAfterStatus(),
            entity.getChangedAt(),
            entity.getCreatedAt()
        );
    }
}
