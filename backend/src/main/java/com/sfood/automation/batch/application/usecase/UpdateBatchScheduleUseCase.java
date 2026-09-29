package com.sfood.automation.batch.application.usecase;

import com.sfood.automation.batch.application.port.BatchSchedulerPort;
import com.sfood.automation.batch.domain.model.BatchScheduleChangeLog;
import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import com.sfood.automation.batch.domain.policy.BatchSchedulePolicy;
import com.sfood.automation.batch.domain.port.BatchScheduleChangeLogRepository;
import com.sfood.automation.batch.domain.port.BatchScheduleConfigRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateBatchScheduleUseCase {

    private final BatchScheduleConfigRepository batchScheduleConfigRepository;
    private final BatchScheduleChangeLogRepository batchScheduleChangeLogRepository;
    private final BatchSchedulerPort batchSchedulerPort;
    private final BatchSchedulePolicy batchSchedulePolicy;

    public UpdateBatchScheduleUseCase(
        BatchScheduleConfigRepository batchScheduleConfigRepository,
        BatchScheduleChangeLogRepository batchScheduleChangeLogRepository,
        BatchSchedulerPort batchSchedulerPort,
        BatchSchedulePolicy batchSchedulePolicy
    ) {
        this.batchScheduleConfigRepository = batchScheduleConfigRepository;
        this.batchScheduleChangeLogRepository = batchScheduleChangeLogRepository;
        this.batchSchedulerPort = batchSchedulerPort;
        this.batchSchedulePolicy = batchSchedulePolicy;
    }

    @Transactional
    public void execute(String jobName, boolean enabled, int fixedDelaySeconds, String modifiedBy) {
        batchSchedulePolicy.validate(fixedDelaySeconds);

        BatchScheduleConfig existing = batchScheduleConfigRepository.findByJobName(jobName).orElseThrow();
        BatchScheduleConfig updated = existing.update(enabled, fixedDelaySeconds, modifiedBy, Instant.now());

        batchScheduleConfigRepository.save(updated);
        batchScheduleChangeLogRepository.save(new BatchScheduleChangeLog(
            null,
            jobName,
            existing.fixedDelaySeconds(),
            updated.fixedDelaySeconds(),
            existing.enabled(),
            updated.enabled(),
            modifiedBy,
            updated.updatedAt()
        ));
        batchSchedulerPort.reschedule(updated);
    }
}
