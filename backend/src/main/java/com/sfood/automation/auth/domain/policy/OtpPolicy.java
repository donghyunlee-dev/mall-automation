package com.sfood.automation.auth.domain.policy;

import com.sfood.automation.auth.domain.port.EmailVerificationRepository;
import com.sfood.automation.common.exception.EmailFormatException;
import com.sfood.automation.common.exception.RateLimitException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OtpPolicy {

    public static final int EXPIRES_IN_SECONDS = 300;

    public void validateEmailDomain(String email) {
        if (email == null || !email.endsWith("@s-food.com")) {
            throw new EmailFormatException();
        }
    }

    public void checkResendLimit(Instant lastSentAt) {
        if (lastSentAt != null && Duration.between(lastSentAt, Instant.now()).getSeconds() < 60) {
            throw new RateLimitException();
        }
    }

    public void checkHourlyLimit(String email, EmailVerificationRepository repository) {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        if (repository.countByEmailAndRequestedAtAfter(email, oneHourAgo) >= 5) {
            throw new RateLimitException();
        }
    }
}
