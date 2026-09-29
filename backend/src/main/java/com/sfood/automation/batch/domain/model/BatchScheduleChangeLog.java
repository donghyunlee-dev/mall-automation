package com.sfood.automation.batch.domain.model;

import java.time.Instant;

public record BatchScheduleChangeLog(
    Integer id,
    String jobName,
    Integer beforeFixedDelaySec,
    Integer afterFixedDelaySec,
    Boolean beforeEnabled,
    Boolean afterEnabled,
    String changedBy,
    Instant changedAt
) {
}
