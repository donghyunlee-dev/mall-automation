package com.sfood.automation.auth.infrastructure.otp;

import com.sfood.automation.auth.application.port.OtpHashPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class OtpHashProvider implements OtpHashPort {

    @Override
    public String hash(String verificationKey, String code, String serverSecret) {
        String source = verificationKey + ":" + code + ":" + serverSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    @Override
    public boolean matches(String verificationKey, String code, String serverSecret, String storedHash) {
        return hash(verificationKey, code, serverSecret).equals(storedHash);
    }
}
