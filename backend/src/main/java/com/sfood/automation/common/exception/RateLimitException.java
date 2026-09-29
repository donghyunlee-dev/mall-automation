package com.sfood.automation.common.exception;

public class RateLimitException extends BusinessException {

    public RateLimitException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
