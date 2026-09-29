package com.sfood.automation.common.exception;

public class MemberPartnerNotFoundException extends BusinessException {

    public MemberPartnerNotFoundException() {
        super(ErrorCode.MEMBER_PARTNER_NOT_FOUND);
    }
}
