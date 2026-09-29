package com.sfood.automation.common.security;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.encryption")
public record EncryptionProperties(String key) {

    public EncryptionProperties {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("app.encryption.key is required");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption.key must be a Base64 encoded value", e);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException("app.encryption.key must decode to 32 bytes (AES-256)");
        }
    }

    public byte[] decodedKey() {
        return Base64.getDecoder().decode(key);
    }
}
