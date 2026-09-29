package com.sfood.automation.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmVerificationRequest(
    @NotBlank String verificationKey,
    @NotBlank @Email String email,
    @NotBlank @Pattern(regexp = "\\d{6}") String code
) {
}
