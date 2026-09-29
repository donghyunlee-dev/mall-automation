package com.sfood.automation.common.exception;

public class OtpAlreadyUsedException extends BusinessException {

    public OtpAlreadyUsedException() {
        super(ErrorCode.OTP_ALREADY_USED);
    }
}
