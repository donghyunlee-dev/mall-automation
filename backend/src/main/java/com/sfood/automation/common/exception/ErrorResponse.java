package com.sfood.automation.common.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(String code, String message, OffsetDateTime timestamp) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.defaultMessage(), OffsetDateTime.now());
    }

    public static ErrorResponse from(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, OffsetDateTime.now());
    }
}
