package com.sfood.automation.batch.infrastructure.persistence;

import com.sfood.automation.batch.domain.model.BatchScheduleChangeLog;
import com.sfood.automation.batch.domain.port.BatchScheduleChangeLogRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class BatchScheduleChangeLogRepositoryImpl implements BatchScheduleChangeLogRepository {

    private final BatchScheduleChangeLogJpaRepository batchScheduleChangeLogJpaRepository;

    public BatchScheduleChangeLogRepositoryImpl(BatchScheduleChangeLogJpaRepository batchScheduleChangeLogJpaRepository) {
        this.batchScheduleChangeLogJpaRepository = batchScheduleChangeLogJpaRepository;
    }

    @Override
    public BatchScheduleChangeLog save(BatchScheduleChangeLog changeLog) {
        return toDomain(batchScheduleChangeLogJpaRepository.save(toEntity(changeLog)));
    }

    private BatchScheduleChangeLogEntity toEntity(BatchScheduleChangeLog changeLog) {
        BatchScheduleChangeLogEntity entity = new BatchScheduleChangeLogEntity();
        entity.setId(changeLog.id());
        entity.setJobName(changeLog.jobName());
        entity.setBeforeFixedDelaySec(changeLog.beforeFixedDelaySec());
        entity.setAfterFixedDelaySec(changeLog.afterFixedDelaySec());
        entity.setBeforeEnabled(toInteger(changeLog.beforeEnabled()));
        entity.setAfterEnabled(toInteger(changeLog.afterEnabled()));
        entity.setChangedBy(changeLog.changedBy());
        entity.setChangedAt(changeLog.changedAt().toString());
        return entity;
    }

    private BatchScheduleChangeLog toDomain(BatchScheduleChangeLogEntity entity) {
        return new BatchScheduleChangeLog(
            entity.getId(),
            entity.getJobName(),
            entity.getBeforeFixedDelaySec(),
            entity.getAfterFixedDelaySec(),
            toBoolean(entity.getBeforeEnabled()),
            toBoolean(entity.getAfterEnabled()),
            entity.getChangedBy(),
            SqliteInstantParser.parse(entity.getChangedAt())
        );
    }

    private Integer toInteger(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private Boolean toBoolean(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1;
    }
}
