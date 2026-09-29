package com.sfood.automation.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmEncryptorTest {

    private static final String TEST_KEY =
        Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final AesGcmEncryptor encryptor = new AesGcmEncryptor(new EncryptionProperties(TEST_KEY));

    @Test
    void should_roundtrip_plaintext() {
        String ciphertext = encryptor.encrypt("홍길동");

        assertThat(ciphertext).isNotEqualTo("홍길동");
        assertThat(encryptor.decrypt(ciphertext)).isEqualTo("홍길동");
    }

    @Test
    void should_return_null_for_null_input() {
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void should_produce_different_ciphertext_for_same_plaintext() {
        assertThat(encryptor.encrypt("A")).isNotEqualTo(encryptor.encrypt("A"));
    }

    @Test
    void should_throw_decryption_exception_for_tampered_ciphertext() {
        String ciphertext = encryptor.encrypt("홍길동");
        byte[] decoded = Base64.getUrlDecoder().decode(ciphertext);
        decoded[decoded.length - 1] ^= 0x01;
        String tampered = Base64.getUrlEncoder().encodeToString(decoded);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
            .isInstanceOf(DecryptionException.class);
    }

    @Test
    void should_roundtrip_empty_string() {
        String ciphertext = encryptor.encrypt("");

        assertThat(encryptor.decrypt(ciphertext)).isEmpty();
    }

    @Test
    void should_fail_fast_when_key_is_invalid() {
        assertThatThrownBy(() -> new EncryptionProperties(null))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EncryptionProperties(" "))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EncryptionProperties("not-base64!!"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EncryptionProperties(
            Base64.getEncoder().encodeToString("short-key".getBytes())))
            .isInstanceOf(IllegalStateException.class);
    }
}
