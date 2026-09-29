package com.sfood.automation.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.auth.domain.enums.VerificationStatus;
import com.sfood.automation.common.exception.OtpAlreadyUsedException;
import com.sfood.automation.common.exception.OtpExpiredException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailVerificationTest {

    @Test
    void should_throw_when_expired() {
        EmailVerification verification = new EmailVerification(
            1, "key", "user@s-food.com", "hash", VerificationStatus.PENDING, 0, 5,
            Instant.now().minusSeconds(400), Instant.now().minusSeconds(1), null,
            Instant.now().minusSeconds(400), null, null, Instant.now().minusSeconds(400), Instant.now().minusSeconds(400)
        );
        assertThatThrownBy(verification::validate).isInstanceOf(OtpExpiredException.class);
    }

    @Test
    void should_throw_when_already_verified() {
        EmailVerification verification = new EmailVerification(
            1, "key", "user@s-food.com", "hash", VerificationStatus.VERIFIED, 0, 5,
            Instant.now(), Instant.now().plusSeconds(300), Instant.now(),
            Instant.now(), null, null, Instant.now(), Instant.now()
        );
        assertThatThrownBy(verification::validate).isInstanceOf(OtpAlreadyUsedException.class);
    }

    @Test
    void should_mark_failed_when_attempts_reach_limit() {
        EmailVerification verification = new EmailVerification(
            1, "key", "user@s-food.com", "hash", VerificationStatus.PENDING, 4, 5,
            Instant.now(), Instant.now().plusSeconds(300), null,
            Instant.now(), null, null, Instant.now(), Instant.now()
        );
        verification.incrementAttempt();
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.FAILED);
    }

    @Test
    void should_mark_verified_and_set_timestamp() {
        EmailVerification verification = new EmailVerification(
            1, "key", "user@s-food.com", "hash", VerificationStatus.PENDING, 0, 5,
            Instant.now(), Instant.now().plusSeconds(300), null,
            Instant.now(), null, null, Instant.now(), Instant.now()
        );
        verification.markVerified();
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(verification.getVerifiedAt()).isNotNull();
    }
}
