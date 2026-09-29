package com.sfood.automation.batch.domain.model;

import com.sfood.automation.batch.domain.enums.BatchScheduleType;
import java.time.Instant;

public record BatchScheduleConfig(
    Integer id,
    String jobName,
    boolean enabled,
    BatchScheduleType scheduleType,
    int fixedDelaySeconds,
    int initialDelaySeconds,
    String description,
    String lastModifiedBy,
    Instant createdAt,
    Instant updatedAt
) {
    public BatchScheduleConfig update(boolean enabled, int fixedDelaySeconds, String modifiedBy, Instant updatedAt) {
        return new BatchScheduleConfig(
            id,
            jobName,
            enabled,
            scheduleType,
            fixedDelaySeconds,
            initialDelaySeconds,
            description,
            modifiedBy,
            createdAt,
            updatedAt
        );
    }
}
