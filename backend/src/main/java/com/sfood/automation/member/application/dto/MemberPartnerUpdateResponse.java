package com.sfood.automation.member.application.dto;

public record MemberPartnerUpdateResponse(
    String partnerCode,
    String partnerName,
    String taxInvoiceFl,
    String memberStatus,
    String bizLicenseFile,
    String updatedAt
) {}
