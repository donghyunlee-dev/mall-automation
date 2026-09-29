package com.sfood.automation.member.application.dto;

import com.sfood.automation.member.domain.enums.MemberUploadType;
import java.util.List;

public record MemberUploadRequest(
    MemberUploadType type,
    String fileName,
    boolean applyDeletion,
    List<List<Object>> rows
) {}
