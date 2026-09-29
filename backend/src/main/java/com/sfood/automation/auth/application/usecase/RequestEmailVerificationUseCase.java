package com.sfood.automation.auth.application.usecase;

import com.sfood.automation.auth.application.dto.EmailVerificationResponse;
import com.sfood.automation.auth.application.port.OtpCodeGenerator;
import com.sfood.automation.auth.application.port.OtpHashPort;
import com.sfood.automation.auth.application.port.OtpMailPort;
import com.sfood.automation.auth.domain.model.EmailVerification;
import com.sfood.automation.auth.domain.policy.OtpPolicy;
import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestEmailVerificationUseCase {

    private final OtpPolicy otpPolicy;
    private final EmailVerificationRepository repository;
    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpHashPort otpHashPort;
    private final OtpMailPort otpMailPort;
    private final String serverSecret;

    public RequestEmailVerificationUseCase(
        OtpPolicy otpPolicy,
        EmailVerificationRepository repository,
        OtpCodeGenerator otpCodeGenerator,
        OtpHashPort otpHashPort,
        OtpMailPort otpMailPort,
        @Value("${app.otp.server-secret}") String serverSecret
    ) {
        this.otpPolicy = otpPolicy;
        this.repository = repository;
        this.otpCodeGenerator = otpCodeGenerator;
        this.otpHashPort = otpHashPort;
        this.otpMailPort = otpMailPort;
        this.serverSecret = serverSecret;
    }

    @Transactional
    public EmailVerificationResponse execute(String email, String requestIp, String userAgent) {
        otpPolicy.validateEmailDomain(email);
        otpPolicy.checkHourlyLimit(email, repository);

        repository.findPendingByEmail(email).ifPresent(existing -> {
            otpPolicy.checkResendLimit(existing.getLastSentAt());
            existing.cancel();
            repository.save(existing);
        });

        String code = otpCodeGenerator.generate();
        EmailVerification created = EmailVerification.createPending(email, "", requestIp, userAgent,
            java.time.Instant.now(), OtpPolicy.EXPIRES_IN_SECONDS);
        String finalCodeHash = otpHashPort.hash(created.getVerificationKey(), code, serverSecret);
        EmailVerification persisted = repository.save(new EmailVerification(
            created.getId(),
            created.getVerificationKey(),
            created.getEmail(),
            finalCodeHash,
            created.getStatus(),
            created.getAttemptCount(),
            created.getMaxAttemptCount(),
            created.getRequestedAt(),
            created.getExpiresAt(),
            created.getVerifiedAt(),
            created.getLastSentAt(),
            created.getRequestIp(),
            created.getUserAgent(),
            created.getCreatedAt(),
            created.getUpdatedAt()
        ));
        otpMailPort.send(email, code);
        return new EmailVerificationResponse(
            persisted.getVerificationKey(),
            OtpPolicy.EXPIRES_IN_SECONDS,
            "Verification code sent"
        );
    }
}
