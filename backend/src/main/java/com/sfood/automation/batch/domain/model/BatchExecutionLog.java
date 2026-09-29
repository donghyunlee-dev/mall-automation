package com.sfood.automation.batch.domain.model;

import com.sfood.automation.batch.domain.enums.BatchExecutionStatus;
import com.sfood.automation.batch.domain.enums.BatchTriggeredBy;
import java.time.Instant;

public record BatchExecutionLog(
    Integer id,
    String jobName,
    Instant startedAt,
    Instant endedAt,
    BatchExecutionStatus status,
    String message,
    BatchTriggeredBy triggeredBy,
    Integer durationMs,
    Instant createdAt
) {
}
