package com.sfood.automation.auth.application.usecase;

import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyPort;
import com.sfood.automation.auth.application.port.AxAuthVerifyResult;
import com.sfood.automation.auth.domain.model.UserAccount;
import com.sfood.automation.auth.domain.policy.OtpPolicy;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.MsLoginVerificationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmMsLoginUseCase {

    private final AxAuthVerifyPort axAuthVerifyPort;
    private final OtpPolicy otpPolicy;
    private final AuthSessionPort authSessionPort;
    private final UserAccountRepository userAccountRepository;

    public ConfirmMsLoginUseCase(
        AxAuthVerifyPort axAuthVerifyPort,
        OtpPolicy otpPolicy,
        AuthSessionPort authSessionPort,
        UserAccountRepository userAccountRepository
    ) {
        this.axAuthVerifyPort = axAuthVerifyPort;
        this.otpPolicy = otpPolicy;
        this.authSessionPort = authSessionPort;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ConfirmVerificationResponse execute(String loginToken, HttpServletRequest request) {
        AxAuthVerifyResult result = axAuthVerifyPort.verify(loginToken);
        if (!result.valid()) {
            throw new MsLoginVerificationFailedException(result.reason());
        }
        // AX-Auth(Azure AD)가 돌려주는 이메일의 대소문자는 OTP 로그인(프론트에서 소문자로 정규화)과
        // 다를 수 있어, 같은 사람이 로그인 방식에 따라 다른 계정으로 분리되지 않도록 여기서도 정규화한다.
        String email = result.email() == null ? null : result.email().trim().toLowerCase();
        otpPolicy.validateEmailDomain(email);
        upsertUserAccount(email);
        authSessionPort.issue(request, email);
        return new ConfirmVerificationResponse(true, email);
    }

    private void upsertUserAccount(String email) {
        Instant now = Instant.now();
        var userAccount = userAccountRepository.findByEmail(email)
            .orElseGet(() -> UserAccount.create(email, now));
        userAccount.markLoggedIn(now);
        userAccountRepository.save(userAccount);
    }
}
