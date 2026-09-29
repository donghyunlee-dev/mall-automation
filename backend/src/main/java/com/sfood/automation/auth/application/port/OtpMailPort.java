package com.sfood.automation.auth.application.port;

public interface OtpMailPort {
    void send(String email, String code);
}
