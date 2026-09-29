package com.sfood.automation.goods.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonStatusConfigJpaRepository extends JpaRepository<CommonStatusConfigEntity, Integer> {
    java.util.List<CommonStatusConfigEntity> findAllByOrderByIdAsc();
    Optional<CommonStatusConfigEntity> findByScope(String scope);
}
