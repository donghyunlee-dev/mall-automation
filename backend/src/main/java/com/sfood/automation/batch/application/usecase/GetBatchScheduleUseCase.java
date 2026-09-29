package com.sfood.automation.batch.application.usecase;

import com.sfood.automation.batch.application.dto.BatchScheduleResponse;
import com.sfood.automation.batch.application.port.BatchSchedulerPort;
import com.sfood.automation.batch.domain.model.BatchExecutionLog;
import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import com.sfood.automation.batch.domain.port.BatchExecutionLogRepository;
import com.sfood.automation.batch.domain.port.BatchScheduleConfigRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBatchScheduleUseCase {

    private final BatchScheduleConfigRepository batchScheduleConfigRepository;
    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final BatchSchedulerPort batchSchedulerPort;

    public GetBatchScheduleUseCase(
        BatchScheduleConfigRepository batchScheduleConfigRepository,
        BatchExecutionLogRepository batchExecutionLogRepository,
        BatchSchedulerPort batchSchedulerPort
    ) {
        this.batchScheduleConfigRepository = batchScheduleConfigRepository;
        this.batchExecutionLogRepository = batchExecutionLogRepository;
        this.batchSchedulerPort = batchSchedulerPort;
    }

    @Transactional(readOnly = true)
    public BatchScheduleResponse execute(String jobName) {
        BatchScheduleConfig config = batchScheduleConfigRepository.findByJobName(jobName).orElseThrow();
        Optional<BatchExecutionLog> latestLog = batchExecutionLogRepository.findLatestByJobName(jobName);
        return new BatchScheduleResponse(
            config.jobName(),
            config.enabled(),
            config.fixedDelaySeconds(),
            batchSchedulerPort.isRunning(jobName),
            latestLog.map(log -> log.startedAt().toString()).orElse(null),
            latestLog.map(log -> log.endedAt() == null ? null : log.endedAt().toString()).orElse(null),
            latestLog.map(log -> log.status().name()).orElse(null),
            latestLog.map(BatchExecutionLog::message).orElse(null)
        );
    }
}
