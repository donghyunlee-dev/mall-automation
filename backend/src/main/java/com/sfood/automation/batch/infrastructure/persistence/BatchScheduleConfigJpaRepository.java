package com.sfood.automation.batch.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchScheduleConfigJpaRepository extends JpaRepository<BatchScheduleConfigEntity, Integer> {
    Optional<BatchScheduleConfigEntity> findByJobName(String jobName);
    List<BatchScheduleConfigEntity> findByEnabled(Integer enabled);
}
