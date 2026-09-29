package com.sfood.automation.auth.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import com.sfood.automation.common.exception.EmailFormatException;
import com.sfood.automation.common.exception.RateLimitException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OtpPolicyTest {

    private final OtpPolicy otpPolicy = new OtpPolicy();

    @Test
    void should_accept_sfood_domain() {
        assertThatCode(() -> otpPolicy.validateEmailDomain("user@s-food.com")).doesNotThrowAnyException();
    }

    @Test
    void should_reject_external_domain() {
        assertThatThrownBy(() -> otpPolicy.validateEmailDomain("user@gmail.com"))
            .isInstanceOf(EmailFormatException.class);
    }

    @Test
    void should_reject_resend_within_sixty_seconds() {
        assertThatThrownBy(() -> otpPolicy.checkResendLimit(Instant.now().minusSeconds(30)))
            .isInstanceOf(RateLimitException.class);
    }

    @Test
    void should_allow_resend_after_sixty_one_seconds() {
        assertThatCode(() -> otpPolicy.checkResendLimit(Instant.now().minusSeconds(61))).doesNotThrowAnyException();
    }

    @Test
    void should_reject_when_hourly_limit_exceeded() {
        EmailVerificationRepository repository = new FakeRepository(5);
        assertThatThrownBy(() -> otpPolicy.checkHourlyLimit("user@s-food.com", repository))
            .isInstanceOf(RateLimitException.class);
    }

    private record FakeRepository(long count) implements EmailVerificationRepository {
        @Override public com.sfood.automation.auth.domain.model.EmailVerification save(com.sfood.automation.auth.domain.model.EmailVerification emailVerification) { return emailVerification; }
        @Override public Optional<com.sfood.automation.auth.domain.model.EmailVerification> findByVerificationKey(String verificationKey) { return Optional.empty(); }
        @Override public Optional<com.sfood.automation.auth.domain.model.EmailVerification> findPendingByEmail(String email) { return Optional.empty(); }
        @Override public long countByEmailAndRequestedAtAfter(String email, Instant from) { return count; }
    }
}
