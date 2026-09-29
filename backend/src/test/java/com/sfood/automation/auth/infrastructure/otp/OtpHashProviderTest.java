package com.sfood.automation.auth.infrastructure.otp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OtpHashProviderTest {

    private final OtpHashProvider provider = new OtpHashProvider();

    @Test
    void should_return_same_hash_for_same_input() {
        assertThat(provider.hash("key", "123456", "secret"))
            .isEqualTo(provider.hash("key", "123456", "secret"));
    }

    @Test
    void should_return_different_hash_for_different_code() {
        assertThat(provider.hash("key", "123456", "secret"))
            .isNotEqualTo(provider.hash("key", "654321", "secret"));
    }

    @Test
    void should_match_correct_code() {
        String hash = provider.hash("key", "123456", "secret");
        assertThat(provider.matches("key", "123456", "secret", hash)).isTrue();
    }

    @Test
    void should_not_match_wrong_code() {
        String hash = provider.hash("key", "123456", "secret");
        assertThat(provider.matches("key", "654321", "secret", hash)).isFalse();
    }
}
