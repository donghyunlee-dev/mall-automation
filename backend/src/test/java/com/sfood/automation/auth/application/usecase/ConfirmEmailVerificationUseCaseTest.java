package com.sfood.automation.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.port.OtpHashPort;
import com.sfood.automation.auth.domain.model.EmailVerification;
import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.OtpMismatchException;
import com.sfood.automation.common.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ConfirmEmailVerificationUseCaseTest {

    @Test
    void should_authenticate_and_issue_session_when_code_matches() {
        EmailVerification verification = EmailVerification.createPending("user@s-food.com", "stored-hash", null, null, Instant.now(), 300);
        FakeRepository repository = new FakeRepository(verification);
        FakeSessionPort sessionPort = new FakeSessionPort();
        FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository();
        ConfirmEmailVerificationUseCase useCase = new ConfirmEmailVerificationUseCase(
            repository, new MatchingHashPort(true), sessionPort, userAccountRepository, "secret"
        );

        var response = useCase.execute(verification.getVerificationKey(), "user@s-food.com", "123456", new MockHttpServletRequest());

        assertThat(response.authenticated()).isTrue();
        assertThat(sessionPort.issuedEmail).isEqualTo("user@s-food.com");
        assertThat(userAccountRepository.saved.getEmail()).isEqualTo("user@s-food.com");
        assertThat(userAccountRepository.saved.getEmailNotificationYn()).isEqualTo("N");
        assertThat(userAccountRepository.saved.getTeamsNotificationYn()).isEqualTo("N");
    }

    @Test
    void should_increment_attempt_and_throw_on_wrong_code() {
        EmailVerification verification = EmailVerification.createPending("user@s-food.com", "stored-hash", null, null, Instant.now(), 300);
        FakeRepository repository = new FakeRepository(verification);
        ConfirmEmailVerificationUseCase useCase = new ConfirmEmailVerificationUseCase(
            repository, new MatchingHashPort(false), new FakeSessionPort(), new FakeUserAccountRepository(), "secret"
        );

        assertThatThrownBy(() -> useCase.execute(verification.getVerificationKey(), "user@s-food.com", "000000", new MockHttpServletRequest()))
            .isInstanceOf(OtpMismatchException.class);
        assertThat(repository.verification.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void should_fail_after_max_attempts() {
        EmailVerification verification = new EmailVerification(
            1, "key", "user@s-food.com", "stored", com.sfood.automation.auth.domain.enums.VerificationStatus.PENDING, 4, 5,
            Instant.now(), Instant.now().plusSeconds(300), null, Instant.now(), null, null, Instant.now(), Instant.now()
        );
        FakeRepository repository = new FakeRepository(verification);
        ConfirmEmailVerificationUseCase useCase = new ConfirmEmailVerificationUseCase(
            repository, new MatchingHashPort(false), new FakeSessionPort(), new FakeUserAccountRepository(), "secret"
        );

        assertThatThrownBy(() -> useCase.execute("key", "user@s-food.com", "000000", new MockHttpServletRequest()))
            .isInstanceOf(RateLimitException.class);
    }

    @Test
    void should_update_last_login_at_when_user_already_exists() {
        EmailVerification verification = EmailVerification.createPending("user@s-food.com", "stored-hash", null, null, Instant.now(), 300);
        FakeRepository repository = new FakeRepository(verification);
        FakeSessionPort sessionPort = new FakeSessionPort();
        FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository(new UserAccount(
            1,
            "user@s-food.com",
            "Y",
            "N",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        ));
        ConfirmEmailVerificationUseCase useCase = new ConfirmEmailVerificationUseCase(
            repository, new MatchingHashPort(true), sessionPort, userAccountRepository, "secret"
        );

        useCase.execute(verification.getVerificationKey(), "user@s-food.com", "123456", new MockHttpServletRequest());

        assertThat(userAccountRepository.saved.getEmailNotificationYn()).isEqualTo("Y");
        assertThat(userAccountRepository.saved.getLastLoginAt()).isAfter(Instant.parse("2026-04-01T00:00:00Z"));
    }

    private static class FakeRepository implements EmailVerificationRepository {
        private final EmailVerification verification;
        private FakeRepository(EmailVerification verification) { this.verification = verification; }
        @Override public EmailVerification save(EmailVerification emailVerification) { return emailVerification; }
        @Override public Optional<EmailVerification> findByVerificationKey(String verificationKey) { return Optional.of(verification); }
        @Override public Optional<EmailVerification> findPendingByEmail(String email) { return Optional.empty(); }
        @Override public long countByEmailAndRequestedAtAfter(String email, Instant from) { return 0; }
    }

    private record MatchingHashPort(boolean result) implements OtpHashPort {
        @Override public String hash(String verificationKey, String code, String serverSecret) { return "hash"; }
        @Override public boolean matches(String verificationKey, String code, String serverSecret, String storedHash) { return result; }
    }

    private static class FakeSessionPort implements AuthSessionPort {
        private String issuedEmail;
        @Override public void issue(HttpServletRequest request, String email) { this.issuedEmail = email; }
        @Override public void invalidate(HttpServletRequest request) { }
    }

    private static class FakeUserAccountRepository implements UserAccountRepository {
        private UserAccount stored;
        private UserAccount saved;

        private FakeUserAccountRepository() {
        }

        private FakeUserAccountRepository(UserAccount stored) {
            this.stored = stored;
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            this.saved = userAccount;
            this.stored = userAccount;
            return userAccount;
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return Optional.ofNullable(stored);
        }

        @Override
        public List<UserAccount> findAllEmailNotificationEnabled() {
            return List.of();
        }
    }
}
