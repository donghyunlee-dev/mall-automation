package com.sfood.automation.batch.infrastructure.persistence;

import com.sfood.automation.batch.domain.enums.BatchExecutionStatus;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import com.sfood.automation.batch.domain.model.BatchExecutionLog;
import com.sfood.automation.batch.domain.port.BatchExecutionLogRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BatchExecutionLogRepositoryImpl implements BatchExecutionLogRepository {

    private final BatchExecutionLogJpaRepository batchExecutionLogJpaRepository;

    public BatchExecutionLogRepositoryImpl(BatchExecutionLogJpaRepository batchExecutionLogJpaRepository) {
        this.batchExecutionLogJpaRepository = batchExecutionLogJpaRepository;
    }

    @Override
    public BatchExecutionLog save(BatchExecutionLog batchExecutionLog) {
        return toDomain(batchExecutionLogJpaRepository.save(toEntity(batchExecutionLog)));
    }

    @Override
    public Optional<BatchExecutionLog> findLatestByJobName(String jobName) {
        return batchExecutionLogJpaRepository.findTopByJobNameOrderByStartedAtDesc(jobName).map(this::toDomain);
    }

    private BatchExecutionLogEntity toEntity(BatchExecutionLog batchExecutionLog) {
        BatchExecutionLogEntity entity = new BatchExecutionLogEntity();
        entity.setId(batchExecutionLog.id());
        entity.setJobName(batchExecutionLog.jobName());
        entity.setStartedAt(batchExecutionLog.startedAt().toString());
        entity.setEndedAt(batchExecutionLog.endedAt() == null ? null : batchExecutionLog.endedAt().toString());
        entity.setStatus(batchExecutionLog.status().name());
        entity.setMessage(batchExecutionLog.message());
        entity.setTriggeredBy(batchExecutionLog.triggeredBy().name());
        entity.setDurationMs(batchExecutionLog.durationMs());
        entity.setCreatedAt(batchExecutionLog.createdAt().toString());
        return entity;
    }

    private BatchExecutionLog toDomain(BatchExecutionLogEntity entity) {
        return new BatchExecutionLog(
            entity.getId(),
            entity.getJobName(),
            SqliteInstantParser.parse(entity.getStartedAt()),
            SqliteInstantParser.parse(entity.getEndedAt()),
            BatchExecutionStatus.valueOf(entity.getStatus()),
            entity.getMessage(),
            BatchTriggeredBy.valueOf(entity.getTriggeredBy()),
            entity.getDurationMs(),
            SqliteInstantParser.parse(entity.getCreatedAt())
        );
    }
}
