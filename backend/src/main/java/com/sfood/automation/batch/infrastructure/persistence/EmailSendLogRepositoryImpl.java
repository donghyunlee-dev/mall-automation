package com.sfood.automation.batch.infrastructure.persistence;

import com.sfood.automation.batch.domain.enums.EmailSendType;
import com.sfood.automation.batch.domain.model.EmailSendLog;
import com.sfood.automation.batch.domain.port.EmailSendLogRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class EmailSendLogRepositoryImpl implements EmailSendLogRepository {

    private final EmailSendLogJpaRepository emailSendLogJpaRepository;

    public EmailSendLogRepositoryImpl(EmailSendLogJpaRepository emailSendLogJpaRepository) {
        this.emailSendLogJpaRepository = emailSendLogJpaRepository;
    }

    @Override
    public EmailSendLog save(EmailSendLog emailSendLog) {
        return toDomain(emailSendLogJpaRepository.save(toEntity(emailSendLog)));
    }

    private EmailSendLogEntity toEntity(EmailSendLog emailSendLog) {
        EmailSendLogEntity entity = new EmailSendLogEntity();
        entity.setId(emailSendLog.id());
        entity.setSendType(emailSendLog.sendType().name());
        entity.setRecipient(emailSendLog.recipient());
        entity.setSubject(emailSendLog.subject());
        entity.setIsSuccess(emailSendLog.isSuccess() ? 1 : 0);
        entity.setErrorMessage(emailSendLog.errorMessage());
        entity.setSentAt(emailSendLog.sentAt().toString());
        entity.setCreatedAt(emailSendLog.createdAt().toString());
        return entity;
    }

    private EmailSendLog toDomain(EmailSendLogEntity entity) {
        return new EmailSendLog(
            entity.getId(),
            EmailSendType.valueOf(entity.getSendType()),
            entity.getRecipient(),
            entity.getSubject(),
            entity.getIsSuccess() == 1,
            entity.getErrorMessage(),
            SqliteInstantParser.parse(entity.getSentAt()),
            SqliteInstantParser.parse(entity.getCreatedAt())
        );
    }
}
