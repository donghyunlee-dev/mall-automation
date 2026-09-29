package com.sfood.automation.goods.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTransactionLogJpaRepository extends JpaRepository<ApiTransactionLogEntity, Integer> {
}
