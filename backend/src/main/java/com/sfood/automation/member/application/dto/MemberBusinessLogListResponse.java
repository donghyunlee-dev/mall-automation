package com.sfood.automation.member.application.dto;

import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogEntity;
import java.util.List;

public record MemberBusinessLogListResponse(List<LogItem> logs) {

    public record LogItem(
        Integer id,
        Long memNo,
        String company,
        String businessNo,
        String ceoName,
        String serviceName,
        String serviceCode,
        String itemName,
        String itemCode,
        String comAddress,
        String bizLicenseFile,
        String loggedAt,
        String loggedBy
    ) {}

    public static MemberBusinessLogListResponse from(
            List<GodoMemberBusinessLogEntity> entities) {
        List<LogItem> items = entities.stream().map(e -> new LogItem(
            e.getId(), e.getMemNo(), e.getCompany(), e.getBusinessNo(),
            e.getCeoName(), e.getServiceName(), e.getServiceCode(),
            e.getItemName(), e.getItemCode(), e.getComAddress(),
            e.getBizLicenseFile(), e.getLoggedAt(), e.getLoggedBy()
        )).toList();
        return new MemberBusinessLogListResponse(items);
    }
}
