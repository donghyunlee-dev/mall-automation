package com.sfood.automation.batch.domain.model;

import com.sfood.automation.batch.domain.enums.EmailSendType;
import java.time.Instant;

public record EmailSendLog(
    Integer id,
    EmailSendType sendType,
    String recipient,
    String subject,
    boolean isSuccess,
    String errorMessage,
    Instant sentAt,
    Instant createdAt
) {
    public static EmailSendLog pending(EmailSendType sendType, String recipient, String subject, Instant now) {
        return new EmailSendLog(null, sendType, recipient, subject, false, null, now, now);
    }

    public EmailSendLog markSuccess(Instant sentAt) {
        return new EmailSendLog(id, sendType, recipient, subject, true, null, sentAt, createdAt);
    }

    public EmailSendLog markFailure(String errorMessage, Instant sentAt) {
        return new EmailSendLog(id, sendType, recipient, subject, false, errorMessage, sentAt, createdAt);
    }
}
