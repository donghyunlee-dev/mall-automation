package com.sfood.automation.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyResult;
import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.policy.OtpPolicy;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.EmailFormatException;
import com.sfood.automation.common.exception.MsLoginVerificationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ConfirmMsLoginUseCaseTest {

    @Test
    void should_issue_session_when_ax_auth_verifies_valid_email() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(true, "user@s-food.com", null));
        FakeSessionPort sessionPort = new FakeSessionPort();
        FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository();
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), sessionPort, userAccountRepository
        );

        var response = useCase.execute("login-token", new MockHttpServletRequest());

        assertThat(response.authenticated()).isTrue();
        assertThat(response.email()).isEqualTo("user@s-food.com");
        assertThat(sessionPort.issuedEmail).isEqualTo("user@s-food.com");
        assertThat(userAccountRepository.saved.getEmail()).isEqualTo("user@s-food.com");
    }

    @Test
    void should_throw_when_ax_auth_reports_invalid_token() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(false, null, "TOKEN_EXPIRED"));
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), new FakeSessionPort(), new FakeUserAccountRepository()
        );

        assertThatThrownBy(() -> useCase.execute("login-token", new MockHttpServletRequest()))
            .isInstanceOf(MsLoginVerificationFailedException.class)
            .hasMessage("TOKEN_EXPIRED");
    }

    @Test
    void should_reject_when_email_domain_is_not_s_food_com() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(true, "user@other.com", null));
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), new FakeSessionPort(), new FakeUserAccountRepository()
        );

        assertThatThrownBy(() -> useCase.execute("login-token", new MockHttpServletRequest()))
            .isInstanceOf(EmailFormatException.class);
    }

    @Test
    void should_normalize_email_case_so_it_matches_the_account_created_via_otp_login() {
        FakeAxAuthVerifyPort axAuthVerifyPort = new FakeAxAuthVerifyPort(new AxAuthVerifyResult(true, "User@S-Food.COM", null));
        FakeSessionPort sessionPort = new FakeSessionPort();
        FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository();
        userAccountRepository.seed("user@s-food.com", new UserAccount(
            1, "user@s-food.com", "Y", "N", java.time.Instant.parse("2026-04-01T00:00:00Z"),
            java.time.Instant.parse("2026-04-01T00:00:00Z"), java.time.Instant.parse("2026-04-01T00:00:00Z")
        ));
        ConfirmMsLoginUseCase useCase = new ConfirmMsLoginUseCase(
            axAuthVerifyPort, new OtpPolicy(), sessionPort, userAccountRepository
        );

        var response = useCase.execute("login-token", new MockHttpServletRequest());

        assertThat(response.email()).isEqualTo("user@s-food.com");
        assertThat(sessionPort.issuedEmail).isEqualTo("user@s-food.com");
        assertThat(userAccountRepository.saved.getEmail()).isEqualTo("user@s-food.com");
        assertThat(userAccountRepository.saved.getEmailNotificationYn()).isEqualTo("Y");
        assertThat(userAccountRepository.accountCount()).isEqualTo(1);
    }

    private record FakeAxAuthVerifyPort(AxAuthVerifyResult result) implements AxAuthVerifyPort {
        @Override
        public AxAuthVerifyResult verify(String loginToken) {
            return result;
        }
    }

    private static class FakeSessionPort implements AuthSessionPort {
        private String issuedEmail;
        @Override public void issue(HttpServletRequest request, String email) { this.issuedEmail = email; }
        @Override public void invalidate(HttpServletRequest request) { }
    }

    private static class FakeUserAccountRepository implements UserAccountRepository {
        private final java.util.Map<String, UserAccount> accountsByEmail = new java.util.HashMap<>();
        private UserAccount saved;

        private void seed(String email, UserAccount userAccount) {
            accountsByEmail.put(email, userAccount);
        }

        private int accountCount() {
            return accountsByEmail.size();
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            this.saved = userAccount;
            accountsByEmail.put(userAccount.getEmail(), userAccount);
            return userAccount;
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return Optional.ofNullable(accountsByEmail.get(email));
        }

        @Override
        public List<UserAccount> findAllEmailNotificationEnabled() {
            return List.of();
        }
    }
}
