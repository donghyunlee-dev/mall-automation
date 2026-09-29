package com.sfood.automation.auth.infrastructure.persistence;

import com.sfood.automation.auth.domain.enums.VerificationStatus;
import com.sfood.automation.auth.domain.model.EmailVerification;
import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import com.sfood.automation.common.time.SqliteInstantParser;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EmailVerificationRepositoryImpl implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository jpaRepository;

    public EmailVerificationRepositoryImpl(EmailVerificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EmailVerification save(EmailVerification emailVerification) {
        return toDomain(jpaRepository.save(toEntity(emailVerification)));
    }

    @Override
    public Optional<EmailVerification> findByVerificationKey(String verificationKey) {
        return jpaRepository.findByVerificationKey(verificationKey).map(this::toDomain);
    }

    @Override
    public Optional<EmailVerification> findPendingByEmail(String email) {
        return jpaRepository.findByEmailAndStatus(email, VerificationStatus.PENDING.name()).map(this::toDomain);
    }

    @Override
    public long countByEmailAndRequestedAtAfter(String email, Instant from) {
        return jpaRepository.countByEmailAndRequestedAtGreaterThanEqual(email, from.toString());
    }

    private EmailVerificationEntity toEntity(EmailVerification domain) {
        EmailVerificationEntity entity = new EmailVerificationEntity();
        entity.setId(domain.getId());
        entity.setVerificationKey(domain.getVerificationKey());
        entity.setEmail(domain.getEmail());
        entity.setCodeHash(domain.getCodeHash());
        entity.setStatus(domain.getStatus().name());
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setMaxAttemptCount(domain.getMaxAttemptCount());
        entity.setRequestedAt(domain.getRequestedAt().toString());
        entity.setExpiresAt(domain.getExpiresAt().toString());
        entity.setVerifiedAt(domain.getVerifiedAt() == null ? null : domain.getVerifiedAt().toString());
        entity.setLastSentAt(domain.getLastSentAt().toString());
        entity.setRequestIp(domain.getRequestIp());
        entity.setUserAgent(domain.getUserAgent());
        entity.setCreatedAt(domain.getCreatedAt().toString());
        entity.setUpdatedAt(domain.getUpdatedAt().toString());
        return entity;
    }

    private EmailVerification toDomain(EmailVerificationEntity entity) {
        return new EmailVerification(
            entity.getId(),
            entity.getVerificationKey(),
            entity.getEmail(),
            entity.getCodeHash(),
            VerificationStatus.valueOf(entity.getStatus()),
            entity.getAttemptCount(),
            entity.getMaxAttemptCount(),
            SqliteInstantParser.parse(entity.getRequestedAt()),
            SqliteInstantParser.parse(entity.getExpiresAt()),
            SqliteInstantParser.parse(entity.getVerifiedAt()),
            SqliteInstantParser.parse(entity.getLastSentAt()),
            entity.getRequestIp(),
            entity.getUserAgent(),
            SqliteInstantParser.parse(entity.getCreatedAt()),
            SqliteInstantParser.parse(entity.getUpdatedAt())
        );
    }
}
