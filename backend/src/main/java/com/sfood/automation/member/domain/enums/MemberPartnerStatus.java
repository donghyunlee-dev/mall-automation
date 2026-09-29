package com.sfood.automation.member.domain.enums;

/** 거래처 상태: 미등록(UNREGISTERED) / 정상(NORMAL) / 불용(INACTIVE) */
public enum MemberPartnerStatus {
    UNREGISTERED, NORMAL, INACTIVE;

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (MemberPartnerStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
