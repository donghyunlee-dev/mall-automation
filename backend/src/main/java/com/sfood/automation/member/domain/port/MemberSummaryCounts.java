package com.sfood.automation.member.domain.port;

public record MemberSummaryCounts(
    long totalCount,
    long businessCount,
    long b2bCount,
    long pendingApprovalCount,
    String lastSyncedAt
) {}
