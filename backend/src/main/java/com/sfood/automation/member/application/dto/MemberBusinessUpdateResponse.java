package com.sfood.automation.member.application.dto;

public record MemberBusinessUpdateResponse(
    String company,
    String businessNo,
    String ceoName,
    String serviceName,
    String serviceCode,
    String itemName,
    String itemCode,
    String comAddress,
    String bizLicenseFile,
    String updatedAt
) {}
