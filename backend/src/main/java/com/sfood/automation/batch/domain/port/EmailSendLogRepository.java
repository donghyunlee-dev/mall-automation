package com.sfood.automation.batch.domain.port;

import com.sfood.automation.batch.domain.model.EmailSendLog;

public interface EmailSendLogRepository {
    EmailSendLog save(EmailSendLog emailSendLog);
}
