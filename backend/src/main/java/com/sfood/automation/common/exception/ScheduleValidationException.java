package com.sfood.automation.common.exception;

public class ScheduleValidationException extends BusinessException {

    public ScheduleValidationException() {
        super(ErrorCode.SCHEDULE_VALIDATION_ERROR);
    }
}
