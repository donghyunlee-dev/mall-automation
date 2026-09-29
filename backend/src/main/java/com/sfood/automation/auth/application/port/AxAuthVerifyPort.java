package com.sfood.automation.auth.application.port;

public interface AxAuthVerifyPort {
    AxAuthVerifyResult verify(String loginToken);
}
