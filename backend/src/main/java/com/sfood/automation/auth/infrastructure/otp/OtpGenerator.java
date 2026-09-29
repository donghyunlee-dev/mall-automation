package com.sfood.automation.auth.infrastructure.otp;

import com.sfood.automation.auth.application.port.OtpCodeGenerator;
import java.security.SecureRandom;
import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class OtpGenerator implements OtpCodeGenerator {

    private final Random random;

    public OtpGenerator() {
        this(new SecureRandom());
    }

    OtpGenerator(Random random) {
        this.random = random;
    }

    @Override
    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
