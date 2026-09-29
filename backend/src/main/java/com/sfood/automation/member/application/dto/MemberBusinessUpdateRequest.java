package com.sfood.automation.member.application.dto;

public record MemberBusinessUpdateRequest(
    String company,
    String businessNo,
    String ceoName,
    String serviceName,
    String serviceCode,
    String itemName,
    String itemCode,
    String comAddress,
    String bizLicenseFile
) {}
