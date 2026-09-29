package com.sfood.automation.member.domain.enums;

public enum MemberGroup {
    GENERAL(1, "일반회원"),
    BUSINESS_WHITE(2, "사업자회원_화이트"),
    BUSINESS_ROSE(3, "사업자회원_로제"),
    BUSINESS_RED(5, "사업자회원_레드"),
    BUSINESS_VIP(6, "사업자회원_VIP"),
    ADMIN(7, "관리자 등급"),
    SKILLED(8, "기능장회원"),
    B2B(9, "B2B 거래처");

    private final int sno;
    private final String label;

    MemberGroup(int sno, String label) {
        this.sno = sno;
        this.label = label;
    }

    public static MemberGroup of(int sno) {
        for (MemberGroup group : values()) {
            if (group.sno == sno) {
                return group;
            }
        }
        return GENERAL;
    }

    public int getSno() {
        return sno;
    }

    public String getLabel() {
        return label;
    }
}
