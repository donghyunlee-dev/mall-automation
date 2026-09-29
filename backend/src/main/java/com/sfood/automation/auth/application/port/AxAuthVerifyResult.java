package com.sfood.automation.auth.application.port;

public record AxAuthVerifyResult(boolean valid, String email, String reason) {
}
