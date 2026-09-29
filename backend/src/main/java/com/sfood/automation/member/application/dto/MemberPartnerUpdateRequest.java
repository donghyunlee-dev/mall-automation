package com.sfood.automation.member.application.dto;

public record MemberPartnerUpdateRequest(
    String partnerCode,
    String partnerName,
    String taxInvoiceFl,
    String memberStatus,
    String bizLicenseFile
) {}
