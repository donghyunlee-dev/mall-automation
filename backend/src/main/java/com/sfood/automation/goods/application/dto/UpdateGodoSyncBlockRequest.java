package com.sfood.automation.goods.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateGodoSyncBlockRequest(
    @NotBlank @Pattern(regexp = "Y|N") String blockedYn
) {
}
