package com.sfood.automation.common.exception;

public class EmailFormatException extends BusinessException {

    public EmailFormatException() {
        super(ErrorCode.EMAIL_FORMAT_INVALID);
    }
}
