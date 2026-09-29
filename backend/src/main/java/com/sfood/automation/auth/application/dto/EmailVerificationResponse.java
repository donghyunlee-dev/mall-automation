package com.sfood.automation.auth.application.dto;

public record EmailVerificationResponse(
    String verificationKey,
    long expiresInSeconds,
    String message
) {
}
