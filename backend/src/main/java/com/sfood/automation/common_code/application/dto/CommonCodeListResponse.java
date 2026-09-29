package com.sfood.automation.common_code.application.dto;

import com.sfood.automation.common_code.infrastructure.persistence.CommonCodeEntity;
import java.util.List;

public record CommonCodeListResponse(List<CodeItem> codes) {

    public record CodeItem(
        Integer id,
        String groupCode,
        String groupName,
        String code,
        String codeName,
        String note,
        String useYn,
        String createdAt,
        String updatedAt
    ) {}

    public static CommonCodeListResponse from(List<CommonCodeEntity> entities) {
        List<CodeItem> items = entities.stream().map(e -> new CodeItem(
            e.getId(), e.getGroupCode(), e.getGroupName(),
            e.getCode(), e.getCodeName(), e.getNote(), e.getUseYn(),
            e.getCreatedAt(), e.getUpdatedAt()
        )).toList();
        return new CommonCodeListResponse(items);
    }
}
