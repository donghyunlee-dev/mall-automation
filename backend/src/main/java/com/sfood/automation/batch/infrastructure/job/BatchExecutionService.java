package com.sfood.automation.batch.infrastructure.job;

import com.sfood.automation.batch.application.port.BatchJobExecutor;
import com.sfood.automation.batch.domain.enums.BatchExecutionStatus;
import com.sfood.automation.batch.domain.enums.BatchJobType;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import java.time.LocalDate;
import com.sfood.automation.batch.domain.model.BatchExecutionLog;
import com.sfood.automation.batch.domain.port.BatchExecutionLogRepository;
import com.sfood.automation.common.slack.SlackNotifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class BatchExecutionService implements BatchJobExecutor {

    private final ProductSyncBatchJob productSyncBatchJob;
    private final MemberSyncBatchJob memberSyncBatchJob;
    private final OrderSyncBatchJob orderSyncBatchJob;
    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final SlackNotifier slackNotifier;
    private final Map<String, AtomicBoolean> runningMap = new ConcurrentHashMap<>();

    public BatchExecutionService(
        ProductSyncBatchJob productSyncBatchJob,
        MemberSyncBatchJob memberSyncBatchJob,
        OrderSyncBatchJob orderSyncBatchJob,
        BatchExecutionLogRepository batchExecutionLogRepository,
        SlackNotifier slackNotifier
    ) {
        this.productSyncBatchJob = productSyncBatchJob;
        this.memberSyncBatchJob = memberSyncBatchJob;
        this.orderSyncBatchJob = orderSyncBatchJob;
        this.batchExecutionLogRepository = batchExecutionLogRepository;
        this.slackNotifier = slackNotifier;
    }

    @Override
    public BatchExecutionStatus execute(String jobName, BatchTriggeredBy triggeredBy) {
        AtomicBoolean running = runningMap.computeIfAbsent(jobName, key -> new AtomicBoolean(false));
        Instant startedAt = Instant.now();

        if (!running.compareAndSet(false, true)) {
            batchExecutionLogRepository.save(new BatchExecutionLog(
                null,
                jobName,
                startedAt,
                startedAt,
                BatchExecutionStatus.SKIPPED,
                "Batch job is already running",
                triggeredBy,
                0,
                startedAt
            ));
            return BatchExecutionStatus.SKIPPED;
        }

        BatchExecutionStatus status = BatchExecutionStatus.SUCCESS;
        String message = null;
        try {
            message = runJob(jobName);
            return BatchExecutionStatus.SUCCESS;
        } catch (Exception exception) {
            status = BatchExecutionStatus.FAIL;
            message = exception.getMessage();
            slackNotifier.send("BatchExecutionService.execute", "[" + jobName + "] " + message);
            return BatchExecutionStatus.FAIL;
        } finally {
            Instant endedAt = Instant.now();
            running.set(false);
            batchExecutionLogRepository.save(new BatchExecutionLog(
                null,
                jobName,
                startedAt,
                endedAt,
                status,
                message,
                triggeredBy,
                Math.toIntExact(Duration.between(startedAt, endedAt).toMillis()),
                startedAt
            ));
        }
    }

    @Override
    public BatchExecutionStatus execute(String jobName, BatchTriggeredBy triggeredBy, LocalDate targetDate) {
        AtomicBoolean running = runningMap.computeIfAbsent(jobName, key -> new AtomicBoolean(false));
        Instant startedAt = Instant.now();

        if (!running.compareAndSet(false, true)) {
            batchExecutionLogRepository.save(new BatchExecutionLog(
                null, jobName, startedAt, startedAt,
                BatchExecutionStatus.SKIPPED, "Batch job is already running",
                triggeredBy, 0, startedAt
            ));
            return BatchExecutionStatus.SKIPPED;
        }

        BatchExecutionStatus status = BatchExecutionStatus.SUCCESS;
        String message = null;
        try {
            message = runJob(jobName, targetDate);
            return BatchExecutionStatus.SUCCESS;
        } catch (Exception exception) {
            status = BatchExecutionStatus.FAIL;
            message = exception.getMessage();
            slackNotifier.send("BatchExecutionService.execute", "[" + jobName + "] " + message);
            return BatchExecutionStatus.FAIL;
        } finally {
            Instant endedAt = Instant.now();
            running.set(false);
            batchExecutionLogRepository.save(new BatchExecutionLog(
                null, jobName, startedAt, endedAt, status, message,
                triggeredBy, Math.toIntExact(Duration.between(startedAt, endedAt).toMillis()), startedAt
            ));
        }
    }

    @Override
    public boolean isRunning(String jobName) {
        return runningMap.computeIfAbsent(jobName, key -> new AtomicBoolean(false)).get();
    }

    private String runJob(String jobName) {
        return runJob(jobName, null);
    }

    private String runJob(String jobName, LocalDate date) {
        if (BatchJobType.PRODUCT_SYNC.jobName().equals(jobName)) {
            return productSyncBatchJob.run();
        }
        if (BatchJobType.MEMBER_SYNC.jobName().equals(jobName)) {
            return date != null ? memberSyncBatchJob.run(date) : memberSyncBatchJob.run();
        }
        if (BatchJobType.ORDER_SYNC.jobName().equals(jobName)) {
            return date != null ? orderSyncBatchJob.run(date) : orderSyncBatchJob.run();
        }
        throw new IllegalArgumentException("Unsupported batch job: " + jobName);
    }
}
