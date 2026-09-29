package com.sfood.automation.member.application.dto;

import java.util.List;

public record MemberUploadCompleteResponse(
    int totalInserted,
    int totalUpdated,
    int totalDeleted,
    int totalFailed,
    List<FailedRow> failedRows,
    boolean deletionSkipped,
    String deletionSkippedReason,
    String syncedAt
) {
    public record FailedRow(int rowIndex, String reason) {}
}
