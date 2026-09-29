package com.sfood.automation.batch.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateBatchScheduleRequest(
    @NotNull Boolean enabled,
    @NotNull Integer fixedDelaySeconds,
    @NotBlank String modifiedBy
) {
}
