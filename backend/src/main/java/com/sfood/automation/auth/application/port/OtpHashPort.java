package com.sfood.automation.auth.application.port;

public interface OtpHashPort {
    String hash(String verificationKey, String code, String serverSecret);

    boolean matches(String verificationKey, String code, String serverSecret, String storedHash);
}
