package com.sfood.automation.common.exception;

public class OtpMismatchException extends BusinessException {

    public OtpMismatchException() {
        super(ErrorCode.OTP_MISMATCH);
    }
}
