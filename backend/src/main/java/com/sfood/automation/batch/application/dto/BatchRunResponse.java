package com.sfood.automation.batch.application.dto;

public record BatchRunResponse(
    boolean triggered,
    String status
) {
}
