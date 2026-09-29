package com.sfood.automation.common.exception;

public class SfoodAdminException extends BusinessException {

    public SfoodAdminException(String message) {
        super(ErrorCode.SFOOD_ADMIN_ERROR, message);
    }

    public SfoodAdminException(String message, Throwable cause) {
        super(ErrorCode.SFOOD_ADMIN_ERROR, message, cause);
    }
}
