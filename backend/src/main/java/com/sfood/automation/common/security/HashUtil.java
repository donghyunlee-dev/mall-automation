package com.sfood.automation.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class HashUtil {

    private static final int SALT_LENGTH_BYTES = 16;

    private final byte[] salt;

    public HashUtil(EncryptionProperties properties) {
        this.salt = Arrays.copyOf(properties.decodedKey(), SALT_LENGTH_BYTES);
    }

    public String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
