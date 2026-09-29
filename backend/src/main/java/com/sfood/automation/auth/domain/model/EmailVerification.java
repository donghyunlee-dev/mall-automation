package com.sfood.automation.auth.domain.model;

import com.sfood.automation.auth.domain.enums.VerificationStatus;
import com.sfood.automation.common.exception.OtpAlreadyUsedException;
import com.sfood.automation.common.exception.OtpExpiredException;
import com.sfood.automation.common.exception.RateLimitException;
import java.time.Instant;
import java.util.UUID;

public class EmailVerification {

    private final Integer id;
    private final String verificationKey;
    private final String email;
    private final String codeHash;
    private VerificationStatus status;
    private int attemptCount;
    private final int maxAttemptCount;
    private final Instant requestedAt;
    private final Instant expiresAt;
    private Instant verifiedAt;
    private final Instant lastSentAt;
    private final String requestIp;
    private final String userAgent;
    private final Instant createdAt;
    private Instant updatedAt;

    public EmailVerification(
        Integer id,
        String verificationKey,
        String email,
        String codeHash,
        VerificationStatus status,
        int attemptCount,
        int maxAttemptCount,
        Instant requestedAt,
        Instant expiresAt,
        Instant verifiedAt,
        Instant lastSentAt,
        String requestIp,
        String userAgent,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.verificationKey = verificationKey;
        this.email = email;
        this.codeHash = codeHash;
        this.status = status;
        this.attemptCount = attemptCount;
        this.maxAttemptCount = maxAttemptCount;
        this.requestedAt = requestedAt;
        this.expiresAt = expiresAt;
        this.verifiedAt = verifiedAt;
        this.lastSentAt = lastSentAt;
        this.requestIp = requestIp;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmailVerification createPending(
        String email,
        String codeHash,
        String requestIp,
        String userAgent,
        Instant now,
        int expiresInSeconds
    ) {
        return new EmailVerification(
            null,
            UUID.randomUUID().toString(),
            email,
            codeHash,
            VerificationStatus.PENDING,
            0,
            5,
            now,
            now.plusSeconds(expiresInSeconds),
            null,
            now,
            requestIp,
            userAgent,
            now,
            now
        );
    }

    public void validate() {
        if (isExpired()) {
            status = VerificationStatus.EXPIRED;
            updatedAt = Instant.now();
            throw new OtpExpiredException();
        }
        if (status == VerificationStatus.VERIFIED || status == VerificationStatus.CANCELLED) {
            throw new OtpAlreadyUsedException();
        }
        if (status == VerificationStatus.FAILED) {
            throw new RateLimitException();
        }
    }

    public void incrementAttempt() {
        attemptCount += 1;
        updatedAt = Instant.now();
        if (attemptCount >= maxAttemptCount) {
            status = VerificationStatus.FAILED;
        }
    }

    public void markVerified() {
        status = VerificationStatus.VERIFIED;
        verifiedAt = Instant.now();
        updatedAt = verifiedAt;
    }

    public void cancel() {
        status = VerificationStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isPending() {
        return status == VerificationStatus.PENDING;
    }

    public Integer getId() {
        return id;
    }

    public String getVerificationKey() {
        return verificationKey;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttemptCount() {
        return maxAttemptCount;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
