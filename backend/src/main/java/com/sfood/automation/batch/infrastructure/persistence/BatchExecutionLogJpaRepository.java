package com.sfood.automation.batch.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExecutionLogJpaRepository extends JpaRepository<BatchExecutionLogEntity, Integer> {
    Optional<BatchExecutionLogEntity> findTopByJobNameOrderByStartedAtDesc(String jobName);
}
