package com.sfood.automation.member.application.dto;

public record MemberSummaryResponse(
    long totalCount,
    long businessCount,
    long b2bCount,
    long pendingApprovalCount,
    String lastSyncedAt
) {}
