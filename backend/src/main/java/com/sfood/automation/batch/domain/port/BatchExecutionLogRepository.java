package com.sfood.automation.batch.domain.port;

import com.sfood.automation.batch.domain.model.BatchExecutionLog;
import java.util.Optional;

public interface BatchExecutionLogRepository {
    BatchExecutionLog save(BatchExecutionLog batchExecutionLog);
    Optional<BatchExecutionLog> findLatestByJobName(String jobName);
}
