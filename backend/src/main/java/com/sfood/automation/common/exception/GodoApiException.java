package com.sfood.automation.common.exception;

public class GodoApiException extends BusinessException {

    public GodoApiException() {
        super(ErrorCode.GODO_API_ERROR);
    }
}
