package com.sfood.automation.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationEntity, Integer> {

    Optional<EmailVerificationEntity> findByVerificationKey(String verificationKey);

    Optional<EmailVerificationEntity> findByEmailAndStatus(String email, String status);

    long countByEmailAndRequestedAtGreaterThanEqual(String email, String requestedAt);
}
