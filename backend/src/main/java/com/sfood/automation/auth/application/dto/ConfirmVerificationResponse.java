package com.sfood.automation.auth.application.dto;

public record ConfirmVerificationResponse(
    boolean authenticated,
    String email
) {
}
