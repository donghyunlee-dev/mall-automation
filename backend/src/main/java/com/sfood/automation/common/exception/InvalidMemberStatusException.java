package com.sfood.automation.common.exception;

public class InvalidMemberStatusException extends BusinessException {

    public InvalidMemberStatusException() {
        super(ErrorCode.MEMBER_STATUS_INVALID);
    }
}
