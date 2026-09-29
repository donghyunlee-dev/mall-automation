package com.sfood.automation.dashboard.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GodoOrderItemJpaRepository extends JpaRepository<GodoOrderItemEntity, Integer> {
    Optional<GodoOrderItemEntity> findByItemTypeAndGodoLineNo(String itemType, Long godoLineNo);
    List<GodoOrderItemEntity> findByOrderNo(Long orderNo);
}
