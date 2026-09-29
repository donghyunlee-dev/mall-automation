package com.sfood.automation.goods.infrastructure.persistence;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GoodsRepositoryImpl implements GoodsRepository {

    private final GoodsJpaRepository goodsJpaRepository;

    public GoodsRepositoryImpl(GoodsJpaRepository goodsJpaRepository) {
        this.goodsJpaRepository = goodsJpaRepository;
    }

    @Override
    public Goods save(Goods goods) {
        return toDomain(goodsJpaRepository.save(toEntity(goods)));
    }

    @Override
    public Optional<Goods> findById(Integer id) {
        return goodsJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Goods> findByGodoCode(String godoGoodsCode) {
        return goodsJpaRepository.findByGodoGoodsCode(godoGoodsCode).map(this::toDomain);
    }

    @Override
    public Optional<Goods> findByErpCode(String erpGoodsCode) {
        return goodsJpaRepository.findByErpGoodsCode(erpGoodsCode).map(this::toDomain);
    }

    @Override
    public List<Goods> findAll(String soldOutYn, String onlyMallYn, String status, String keyword) {
        return goodsJpaRepository.findAll().stream()
            .filter(entity -> !"HIDE".equals(entity.getStatus()))
            .filter(entity -> soldOutYn == null || soldOutYn.equals(entity.getSoldOutYn()))
            .filter(entity -> onlyMallYn == null || onlyMallYn.equals(entity.getOnlyMallYn()))
            .filter(entity -> status == null || status.equals(entity.getStatus()))
            .filter(entity -> keyword == null || entity.getGoodsName().contains(keyword) || entity.getGodoGoodsCode().contains(keyword))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Goods> findAllByOnlyMallYn(String onlyMallYn) {
        return goodsJpaRepository.findAll().stream()
            .filter(entity -> onlyMallYn.equals(entity.getOnlyMallYn()))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Goods> findAllErpSyncNGoods() {
        return goodsJpaRepository.findAll().stream()
            .filter(entity -> "N".equals(entity.getErpSyncYn()))
            .filter(entity -> !"HIDE".equals(entity.getStatus()))
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void delete(Goods goods) {
        goodsJpaRepository.delete(toEntity(goods));
    }

    private GoodsEntity toEntity(Goods goods) {
        GoodsEntity entity = new GoodsEntity();
        entity.setId(goods.getId());
        entity.setGodoGoodsCode(goods.getGodoGoodsCode());
        entity.setErpGoodsCode(goods.getErpGoodsCode());
        entity.setGoodsName(goods.getGoodsName());
        entity.setOnlyMallYn(goods.getOnlyMallYn());
        entity.setSoldOutYn(goods.getSoldOutYn());
        entity.setAvailableQty(goods.getAvailableQty());
        entity.setSafetyRate(goods.getSafetyRate());
        entity.setRemainValidDays(goods.getRemainValidDays());
        entity.setGodoSyncYn(goods.getGodoSyncYn());
        entity.setErpSyncYn(goods.getErpSyncYn());
        entity.setGodoSyncBlockedYn(goods.getGodoSyncBlockedYn());
        entity.setDescription(goods.getDescription());
        entity.setMessage(goods.getMessage());
        entity.setStatus(goods.getStatus().name());
        entity.setCreatedId(goods.getCreatedId());
        entity.setCreatedAt(goods.getCreatedAt().toString());
        entity.setUpdatedId(goods.getUpdatedId());
        entity.setUpdatedAt(goods.getUpdatedAt() == null ? null : goods.getUpdatedAt().toString());
        return entity;
    }

    private Goods toDomain(GoodsEntity entity) {
        return new Goods(
            entity.getId(),
            entity.getGodoGoodsCode(),
            entity.getErpGoodsCode(),
            entity.getGoodsName(),
            entity.getOnlyMallYn(),
            entity.getSoldOutYn(),
            entity.getAvailableQty(),
            entity.getSafetyRate(),
            entity.getRemainValidDays(),
            entity.getGodoSyncYn(),
            entity.getErpSyncYn(),
            entity.getGodoSyncBlockedYn(),
            entity.getDescription(),
            entity.getMessage(),
            GoodsStatus.valueOf(entity.getStatus()),
            entity.getCreatedId(),
            SqliteInstantParser.parse(entity.getCreatedAt()),
            entity.getUpdatedId(),
            SqliteInstantParser.parse(entity.getUpdatedAt())
        );
    }
}
