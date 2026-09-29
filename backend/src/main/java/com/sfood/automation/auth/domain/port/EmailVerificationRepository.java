package com.sfood.automation.auth.domain.port;

import com.sfood.automation.auth.domain.model.EmailVerification;
import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationRepository {

    EmailVerification save(EmailVerification emailVerification);

    Optional<EmailVerification> findByVerificationKey(String verificationKey);

    Optional<EmailVerification> findPendingByEmail(String email);

    long countByEmailAndRequestedAtAfter(String email, Instant from);
}
