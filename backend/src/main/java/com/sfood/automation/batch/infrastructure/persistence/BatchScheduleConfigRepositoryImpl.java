package com.sfood.automation.batch.infrastructure.persistence;

import com.sfood.automation.batch.domain.enums.BatchScheduleType;
import com.sfood.automation.batch.domain.model.BatchScheduleConfig;
import com.sfood.automation.batch.domain.port.BatchScheduleConfigRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BatchScheduleConfigRepositoryImpl implements BatchScheduleConfigRepository {

    private final BatchScheduleConfigJpaRepository batchScheduleConfigJpaRepository;

    public BatchScheduleConfigRepositoryImpl(BatchScheduleConfigJpaRepository batchScheduleConfigJpaRepository) {
        this.batchScheduleConfigJpaRepository = batchScheduleConfigJpaRepository;
    }

    @Override
    public Optional<BatchScheduleConfig> findByJobName(String jobName) {
        return batchScheduleConfigJpaRepository.findByJobName(jobName).map(this::toDomain);
    }

    @Override
    public List<BatchScheduleConfig> findAllByEnabledTrue() {
        return batchScheduleConfigJpaRepository.findByEnabled(1).stream().map(this::toDomain).toList();
    }

    @Override
    public BatchScheduleConfig save(BatchScheduleConfig batchScheduleConfig) {
        return toDomain(batchScheduleConfigJpaRepository.save(toEntity(batchScheduleConfig)));
    }

    private BatchScheduleConfigEntity toEntity(BatchScheduleConfig batchScheduleConfig) {
        BatchScheduleConfigEntity entity = new BatchScheduleConfigEntity();
        entity.setId(batchScheduleConfig.id());
        entity.setJobName(batchScheduleConfig.jobName());
        entity.setEnabled(batchScheduleConfig.enabled() ? 1 : 0);
        entity.setScheduleType(batchScheduleConfig.scheduleType().name());
        entity.setFixedDelaySeconds(batchScheduleConfig.fixedDelaySeconds());
        entity.setInitialDelaySeconds(batchScheduleConfig.initialDelaySeconds());
        entity.setDescription(batchScheduleConfig.description());
        entity.setLastModifiedBy(batchScheduleConfig.lastModifiedBy());
        entity.setCreatedAt(batchScheduleConfig.createdAt().toString());
        entity.setUpdatedAt(batchScheduleConfig.updatedAt().toString());
        return entity;
    }

    private BatchScheduleConfig toDomain(BatchScheduleConfigEntity entity) {
        return new BatchScheduleConfig(
            entity.getId(),
            entity.getJobName(),
            entity.getEnabled() == 1,
            BatchScheduleType.valueOf(entity.getScheduleType()),
            entity.getFixedDelaySeconds(),
            entity.getInitialDelaySeconds(),
            entity.getDescription(),
            entity.getLastModifiedBy(),
            SqliteInstantParser.parse(entity.getCreatedAt()),
            SqliteInstantParser.parse(entity.getUpdatedAt())
        );
    }
}
