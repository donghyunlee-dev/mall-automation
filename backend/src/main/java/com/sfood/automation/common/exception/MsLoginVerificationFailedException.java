package com.sfood.automation.common.exception;

public class MsLoginVerificationFailedException extends BusinessException {

    public MsLoginVerificationFailedException(String reason) {
        super(ErrorCode.MS_LOGIN_VERIFICATION_FAILED, reason != null ? reason : ErrorCode.MS_LOGIN_VERIFICATION_FAILED.defaultMessage());
    }
}
