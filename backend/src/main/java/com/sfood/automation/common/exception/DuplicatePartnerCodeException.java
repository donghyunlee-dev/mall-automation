package com.sfood.automation.common.exception;

public class DuplicatePartnerCodeException extends BusinessException {

    public DuplicatePartnerCodeException() {
        super(ErrorCode.DUPLICATE_PARTNER_CODE);
    }
}
