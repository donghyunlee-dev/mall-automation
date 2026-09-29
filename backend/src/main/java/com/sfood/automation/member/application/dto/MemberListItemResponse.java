package com.sfood.automation.member.application.dto;

public record MemberListItemResponse(
    long memNo,
    String memId,
    String memName,
    int groupSno,
    String groupName,
    String email,
    String cellPhone,
    String company,
    String businessNo,
    String ceoName,
    String serviceName,
    String itemName,
    String comAddress,
    String partnerCode,
    String partnerName,
    String memberStatus,
    String entryDt,
    String appFl,
    String duplicateMemNo
) {}
