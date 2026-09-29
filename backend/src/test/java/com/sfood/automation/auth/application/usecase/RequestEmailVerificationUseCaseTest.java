package com.sfood.automation.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.auth.application.dto.EmailVerificationResponse;
import com.sfood.automation.auth.application.port.OtpCodeGenerator;
import com.sfood.automation.auth.application.port.OtpHashPort;
import com.sfood.automation.auth.application.port.OtpMailPort;
import com.sfood.automation.auth.domain.model.EmailVerification;
import com.sfood.automation.auth.domain.policy.OtpPolicy;
import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RequestEmailVerificationUseCaseTest {

    @Test
    void should_create_verification_and_store_hash_only() {
        FakeRepository repository = new FakeRepository();
        RequestEmailVerificationUseCase useCase = new RequestEmailVerificationUseCase(
            new OtpPolicy(),
            repository,
            () -> "123456",
            new FakeHasher(),
            (email, code) -> {},
            "secret"
        );

        EmailVerificationResponse response = useCase.execute("user@s-food.com", "127.0.0.1", "test");

        assertThat(response.verificationKey()).isNotBlank();
        assertThat(repository.saved.getCodeHash()).isEqualTo("hashed");
        assertThat(repository.saved.getCodeHash()).doesNotContain("123456");
    }

    @Test
    void should_cancel_existing_pending_session_when_re_requesting() {
        FakeRepository repository = new FakeRepository();
        repository.pending = EmailVerification.createPending("user@s-food.com", "old", null, null, Instant.now().minusSeconds(61), 300);
        RequestEmailVerificationUseCase useCase = new RequestEmailVerificationUseCase(
            new OtpPolicy(), repository, () -> "123456", new FakeHasher(), (email, code) -> {}, "secret"
        );

        useCase.execute("user@s-food.com", null, null);

        assertThat(repository.cancelled).isTrue();
    }

    @Test
    void should_not_store_raw_code() {
        FakeRepository repository = new FakeRepository();
        RequestEmailVerificationUseCase useCase = new RequestEmailVerificationUseCase(
            new OtpPolicy(), repository, () -> "654321", new FakeHasher(), (email, code) -> {}, "secret"
        );
        useCase.execute("user@s-food.com", null, null);
        assertThat(repository.saved.getCodeHash()).isEqualTo("hashed");
    }

    private static class FakeRepository implements EmailVerificationRepository {
        private final Map<String, EmailVerification> storage = new HashMap<>();
        private EmailVerification pending;
        private EmailVerification saved;
        private boolean cancelled;

        @Override
        public EmailVerification save(EmailVerification emailVerification) {
            if (pending != null && pending.getVerificationKey().equals(emailVerification.getVerificationKey())
                && emailVerification.getStatus() == com.sfood.automation.auth.domain.enums.VerificationStatus.CANCELLED) {
                cancelled = true;
            }
            storage.put(emailVerification.getVerificationKey(), emailVerification);
            saved = emailVerification;
            return emailVerification;
        }

        @Override
        public Optional<EmailVerification> findByVerificationKey(String verificationKey) {
            return Optional.ofNullable(storage.get(verificationKey));
        }

        @Override
        public Optional<EmailVerification> findPendingByEmail(String email) {
            return Optional.ofNullable(pending);
        }

        @Override
        public long countByEmailAndRequestedAtAfter(String email, Instant from) {
            return 0;
        }
    }

    private static class FakeHasher implements OtpHashPort {
        @Override public String hash(String verificationKey, String code, String serverSecret) { return "hashed"; }
        @Override public boolean matches(String verificationKey, String code, String serverSecret, String storedHash) { return false; }
    }
}
