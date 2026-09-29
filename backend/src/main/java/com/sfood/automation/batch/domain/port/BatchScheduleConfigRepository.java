package com.sfood.automation.batch.domain.port;

import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import java.util.List;
import java.util.Optional;

public interface BatchScheduleConfigRepository {
    Optional<BatchScheduleConfig> findByJobName(String jobName);
    List<BatchScheduleConfig> findAllByEnabledTrue();
    BatchScheduleConfig save(BatchScheduleConfig batchScheduleConfig);
}
