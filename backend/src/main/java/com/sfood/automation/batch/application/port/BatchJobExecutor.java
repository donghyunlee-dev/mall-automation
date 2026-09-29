package com.sfood.automation.batch.application.port;

import com.sfood.automation.batch.domain.enums.BatchExecutionStatus;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import java.time.LocalDate;

public interface BatchJobExecutor {
    BatchExecutionStatus execute(String jobName, BatchTriggeredBy triggeredBy);
    boolean isRunning(String jobName);

    default BatchExecutionStatus execute(String jobName, BatchTriggeredBy triggeredBy, LocalDate targetDate) {
        return execute(jobName, triggeredBy);
    }
}
