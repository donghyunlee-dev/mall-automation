package com.sfood.automation.auth.application.usecase;

import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.port.OtpHashPort;
import com.sfood.automation.auth.domain.model.EmailVerification;
import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import com.sfood.automation.auth.domain.port.UserAccountRepository;
import com.sfood.automation.common.exception.OtpMismatchException;
import com.sfood.automation.common.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmEmailVerificationUseCase {

    private final EmailVerificationRepository repository;
    private final OtpHashPort otpHashPort;
    private final AuthSessionPort authSessionPort;
    private final UserAccountRepository userAccountRepository;
    private final String serverSecret;

    public ConfirmEmailVerificationUseCase(
        EmailVerificationRepository repository,
        OtpHashPort otpHashPort,
        AuthSessionPort authSessionPort,
        UserAccountRepository userAccountRepository,
        @Value("${app.otp.server-secret}") String serverSecret
    ) {
        this.repository = repository;
        this.otpHashPort = otpHashPort;
        this.authSessionPort = authSessionPort;
        this.userAccountRepository = userAccountRepository;
        this.serverSecret = serverSecret;
    }

    @Transactional
    public ConfirmVerificationResponse execute(
        String verificationKey,
        String email,
        String code,
        HttpServletRequest request
    ) {
        EmailVerification verification = repository.findByVerificationKey(verificationKey)
            .orElseThrow(OtpMismatchException::new);
        verification.validate();

        boolean matched = otpHashPort.matches(verificationKey, code, serverSecret, verification.getCodeHash());
        if (!matched || !verification.getEmail().equals(email)) {
            verification.incrementAttempt();
            repository.save(verification);
            if (verification.getStatus() == com.sfood.automation.auth.domain.enums.VerificationStatus.FAILED) {
                throw new RateLimitException();
            }
            throw new OtpMismatchException();
        }

        verification.markVerified();
        repository.save(verification);
        upsertUserAccount(verification.getEmail());
        authSessionPort.issue(request, verification.getEmail());
        return new ConfirmVerificationResponse(true, verification.getEmail());
    }

    private void upsertUserAccount(String email) {
        Instant now = Instant.now();
        var userAccount = userAccountRepository.findByEmail(email)
            .orElseGet(() -> com.sfood.automation.auth.domain.model.UserAccount.create(email, now));
        userAccount.markLoggedIn(now);
        userAccountRepository.save(userAccount);
    }
}
