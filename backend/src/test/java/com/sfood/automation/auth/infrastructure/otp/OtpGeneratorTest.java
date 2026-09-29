package com.sfood.automation.auth.infrastructure.otp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class OtpGeneratorTest {

    @Test
    void should_generate_six_digit_string() {
        OtpGenerator generator = new OtpGenerator(new Random(1));
        assertThat(generator.generate()).matches("\\d{6}");
    }

    @Test
    void should_preserve_leading_zero() {
        OtpGenerator generator = new OtpGenerator(new Random() {
            @Override
            public int nextInt(int bound) {
                return 123;
            }
        });
        assertThat(generator.generate()).isEqualTo("000123");
    }

    @Test
    void should_generate_numeric_six_digit_values_repeatedly() {
        OtpGenerator generator = new OtpGenerator(new Random(7));
        for (int i = 0; i < 100; i++) {
            assertThat(generator.generate()).matches("\\d{6}");
        }
    }
}
