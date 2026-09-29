package com.sfood.automation.goods.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsJpaRepository extends JpaRepository<GoodsEntity, Integer> {
    Optional<GoodsEntity> findByGodoGoodsCode(String godoGoodsCode);
    Optional<GoodsEntity> findByErpGoodsCode(String erpGoodsCode);
}
