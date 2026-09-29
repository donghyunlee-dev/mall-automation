package com.sfood.automation.member.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.CrawledMember;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SfoodAdminMemberDetailCollectorTest {

    private SfoodAdminMemberDetailCollector collector;
    private CrawledMember member;

    @BeforeEach
    void setUp() throws IOException {
        collector = new SfoodAdminMemberDetailCollector();
        String html = new ClassPathResource("fixtures/member_modify_page.html")
                .getContentAsString(StandardCharsets.UTF_8);
        member = collector.collect(html, 10001L);
    }

    @Test
    void should_parse_mem_no_and_mem_id() {
        assertThat(member.memNo()).isEqualTo(10001L);
        assertThat(member.memId()).isEqualTo("testuser1");
    }

    @Test
    void should_parse_group_and_status() {
        assertThat(member.groupSno()).isEqualTo(9);
        assertThat(member.memberFl()).isEqualTo("business");
        assertThat(member.appFl()).isEqualTo("y");
    }

    @Test
    void should_parse_name_and_nickname() {
        assertThat(member.memName()).isEqualTo("홍길동");
        assertThat(member.nickName()).isEqualTo("테스트닉");
    }

    @Test
    void should_assemble_email_from_two_inputs() {
        assertThat(member.email()).isEqualTo("testuser1@test.com");
    }

    @Test
    void should_parse_contact_info() {
        assertThat(member.cellPhone()).isEqualTo("01011112222");
        assertThat(member.phone()).isEqualTo("0212345678");
    }

    @Test
    void should_combine_address_parts() {
        assertThat(member.address()).isEqualTo("06000 서울시 강남구 테헤란로 1 101호");
    }

    @Test
    void should_fall_back_to_address_only_when_zonecode_missing() {
        String html = "<input type=\"text\" name=\"address\" value=\"서울시 강남구 테헤란로 1\">"
            + "<input type=\"text\" name=\"addressSub\" value=\"101호\">";
        CrawledMember result = collector.collect(html, 10001L);

        assertThat(result.address()).isEqualTo("서울시 강남구 테헤란로 1 101호");
    }

    @Test
    void should_parse_checkbox_as_y_or_n() {
        assertThat(member.smsFl()).isEqualTo("y");
        assertThat(member.maillingFl()).isEqualTo("n");
    }

    @Test
    void should_parse_readonly_text_fields() {
        assertThat(member.entryDt()).isEqualTo("2026-06-24 09:00:00");
        assertThat(member.lastLoginDt()).isEqualTo("2026-06-24 10:00:00");
        assertThat(member.entryPath()).isEqualTo("pc");
        assertThat(member.loginCount()).isEqualTo(3);
    }

    @Test
    void should_parse_amount_fields() {
        assertThat(member.saleAmount()).isEqualTo(500000.0);
        assertThat(member.mileage()).isEqualTo(1000.0);
        assertThat(member.deposit()).isEqualTo(200.0);
    }

    @Test
    void should_parse_business_info() {
        assertThat(member.company()).isEqualTo("(주)테스트");
        assertThat(member.businessNo()).isEqualTo("1234567890");
        assertThat(member.ceoName()).isEqualTo("김대표");
        assertThat(member.service()).isEqualTo("도매");
        assertThat(member.item()).isEqualTo("식품");
    }

    @Test
    void should_combine_com_address_parts() {
        assertThat(member.comAddress()).isEqualTo("12345 경기도 수원시 팔달구 인계로 1 3층 301호");
    }

    @Test
    void should_parse_extra_fields() {
        assertThat(member.ex1()).isEqualTo("치킨");
        assertThat(member.ex2()).isEqualTo("필요없음(기본)");
        assertThat(member.ex3()).isEqualTo("박담당");
    }

    @Test
    void should_join_checked_interest_checkboxes() {
        assertThat(member.interest()).isEqualTo("01001003,01001004");
    }

    @Test
    void should_parse_personal_info() {
        assertThat(member.sexFl()).isEqualTo("m");
        assertThat(member.birthDt()).isEqualTo("1990-01-01");
        assertThat(member.marriFl()).isEqualTo("n");
        assertThat(member.recommId()).isEqualTo("refuser1");
        assertThat(member.expirationFl()).isEqualTo(1);
    }

    @Test
    void should_return_null_for_zero_date_marri_date() {
        assertThat(member.marriDate()).isNull();
    }

    @Test
    void should_parse_memo_from_textarea() {
        assertThat(member.memo()).isEqualTo("테스트 메모입니다.");
    }

    @Test
    void should_return_null_appDt_always() {
        assertThat(member.appDt()).isNull();
    }
}
