package com.sfood.automation.common_code.application.dto;

public record CommonCodeRequest(
    String groupCode,
    String groupName,
    String code,
    String codeName,
    String note,
    String useYn
) {}
