package com.sfood.automation.member.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberMaskerTest {

    private final MemberMasker masker = new MemberMasker();

    @Test
    void should_mask_name_by_length() {
        assertThat(masker.maskName("홍")).isEqualTo("홍");
        assertThat(masker.maskName("홍길")).isEqualTo("홍*");
        assertThat(masker.maskName("홍길동")).isEqualTo("홍*동");
        assertThat(masker.maskName("홍길동이")).isEqualTo("홍**이");
        assertThat(masker.maskName(null)).isNull();
    }

    @Test
    void should_mask_email_local_part() {
        assertThat(masker.maskEmail("hong@naver.com")).isEqualTo("ho***@naver.com");
        assertThat(masker.maskEmail("h@test.com")).isEqualTo("*@test.com");
        assertThat(masker.maskEmail(null)).isNull();
        assertThat(masker.maskEmail("invalid-email")).isEqualTo("***");
    }

    @Test
    void should_mask_cell_phone_middle_digits() {
        assertThat(masker.maskCellPhone("010-1234-5678")).isEqualTo("010-****-5678");
        assertThat(masker.maskCellPhone("01012345678")).isEqualTo("010****5678");
        assertThat(masker.maskCellPhone(null)).isNull();
        assertThat(masker.maskCellPhone("1234")).isEqualTo("****");
    }
}
