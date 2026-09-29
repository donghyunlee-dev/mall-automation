package com.sfood.automation.batch.application.usecase;

import com.sfood.automation.batch.application.dto.BatchRunResponse;
import com.sfood.automation.batch.application.port.BatchJobExecutor;
import com.sfood.automation.batch.domain.enums.BatchExecutionStatus;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class RunBatchNowUseCase {

    private final BatchJobExecutor batchJobExecutor;

    public RunBatchNowUseCase(BatchJobExecutor batchJobExecutor) {
        this.batchJobExecutor = batchJobExecutor;
    }

    public BatchRunResponse execute(String jobName) {
        BatchExecutionStatus status = batchJobExecutor.execute(jobName, BatchTriggeredBy.MANUAL);
        if (status == BatchExecutionStatus.SKIPPED) {
            return new BatchRunResponse(false, status.name());
        }
        return new BatchRunResponse(true, "STARTED");
    }

    public BatchRunResponse execute(String jobName, LocalDate targetDate) {
        BatchExecutionStatus status = batchJobExecutor.execute(jobName, BatchTriggeredBy.MANUAL, targetDate);
        if (status == BatchExecutionStatus.SKIPPED) {
            return new BatchRunResponse(false, status.name());
        }
        return new BatchRunResponse(true, "STARTED");
    }
}
