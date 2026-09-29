package com.sfood.automation.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class HashUtilTest {

    private static final String TEST_KEY =
        Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final HashUtil hashUtil = new HashUtil(new EncryptionProperties(TEST_KEY));

    @Test
    void should_return_same_hash_for_same_input() {
        assertThat(hashUtil.hash("01012345678")).isEqualTo(hashUtil.hash("01012345678"));
    }

    @Test
    void should_return_different_hash_for_different_input() {
        assertThat(hashUtil.hash("A")).isNotEqualTo(hashUtil.hash("B"));
    }

    @Test
    void should_return_lowercase_hex_64_chars() {
        assertThat(hashUtil.hash("홍길동")).matches("^[0-9a-f]{64}$");
    }

    @Test
    void should_return_null_for_null_or_blank_input() {
        assertThat(hashUtil.hash(null)).isNull();
        assertThat(hashUtil.hash("")).isNull();
        assertThat(hashUtil.hash("   ")).isNull();
    }

    @Test
    void should_apply_salt() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String unsalted = HexFormat.of().formatHex(digest.digest("A".getBytes(StandardCharsets.UTF_8)));

        assertThat(hashUtil.hash("A")).isNotEqualTo(unsalted);
    }
}
