package com.sfood.automation.batch.application.dto;

public record BatchScheduleResponse(
    String jobName,
    boolean enabled,
    int fixedDelaySeconds,
    boolean running,
    String lastStartedAt,
    String lastEndedAt,
    String lastStatus,
    String lastMessage
) {
}
