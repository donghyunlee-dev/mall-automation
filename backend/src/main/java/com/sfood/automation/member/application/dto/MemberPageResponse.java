package com.sfood.automation.member.application.dto;

import java.util.List;

public record MemberPageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
