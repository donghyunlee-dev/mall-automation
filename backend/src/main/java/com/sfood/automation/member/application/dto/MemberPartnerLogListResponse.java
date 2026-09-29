package com.sfood.automation.member.application.dto;

import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
import java.util.List;

public record MemberPartnerLogListResponse(List<LogItem> logs) {

    public record LogItem(
        Integer id,
        Long memNo,
        String partnerCode,
        String partnerName,
        String taxInvoiceFl,
        String memberStatus,
        String loggedAt,
        String loggedBy
    ) {}

    public static MemberPartnerLogListResponse from(List<MemberPartnerLogEntity> entities) {
        List<LogItem> items = entities.stream().map(e -> new LogItem(
            e.getId(), e.getMemNo(), e.getPartnerCode(),
            e.getPartnerName(), e.getTaxInvoiceFl(), e.getMemberStatus(),
            e.getLoggedAt(), e.getLoggedBy()
        )).toList();
        return new MemberPartnerLogListResponse(items);
    }
}
