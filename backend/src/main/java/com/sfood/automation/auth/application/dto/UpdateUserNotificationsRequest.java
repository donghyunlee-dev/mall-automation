package com.sfood.automation.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserNotificationsRequest(
    @NotBlank @Pattern(regexp = "Y|N") String emailNotificationYn,
    @NotBlank @Pattern(regexp = "Y|N") String teamsNotificationYn
) {
}
