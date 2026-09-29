package com.sfood.automation.member.application.dto;

public record MemberPartnerDetailResponse(
    String partnerCode,
    String partnerName,
    String taxInvoiceFl,
    String memberStatus,
    String updatedAt
) {}
