package com.sfood.automation.batch.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchScheduleChangeLogJpaRepository extends JpaRepository<BatchScheduleChangeLogEntity, Integer> {
}
