package com.sfood.automation.dashboard.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GodoOrderJpaRepository extends JpaRepository<GodoOrderEntity, Integer> {
    Optional<GodoOrderEntity> findByOrderNo(Long orderNo);
}
