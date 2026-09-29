package com.sfood.automation.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record MsLoginVerifyRequest(@NotBlank String loginToken) {
}
