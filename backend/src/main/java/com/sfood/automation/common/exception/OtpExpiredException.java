package com.sfood.automation.common.exception;

public class OtpExpiredException extends BusinessException {

    public OtpExpiredException() {
        super(ErrorCode.OTP_EXPIRED);
    }
}
