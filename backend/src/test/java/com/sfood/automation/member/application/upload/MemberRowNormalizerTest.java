package com.sfood.automation.member.application.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.common.security.EncryptionProperties;
import com.sfood.automation.common.security.HashUtil;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberRowNormalizerTest {

    private static final String TEST_KEY =
        Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final HashUtil hashUtil = new HashUtil(new EncryptionProperties(TEST_KEY));
    private final MemberRowNormalizer normalizer = new MemberRowNormalizer(hashUtil);

    // Excel serial 45292 = 2024-01-01 (검증 앵커: serial 44562 = 2022-01-01 + 730일)
    @Test
    void should_convert_serial_number_to_date() {
        assertThat(normalizer.normalizeDate(45292.0)).isEqualTo("2024-01-01");
        assertThat(normalizer.normalizeDate(45292)).isEqualTo("2024-01-01");
    }

    @Test
    void should_extract_date_part_from_datetime_string() {
        assertThat(normalizer.normalizeDate("2025-01-15 10:30:00")).isEqualTo("2025-01-15");
        assertThat(normalizer.normalizeDate("1990-01-01")).isEqualTo("1990-01-01");
    }

    @Test
    void should_return_null_for_empty_date() {
        assertThat(normalizer.normalizeDate(null)).isNull();
        assertThat(normalizer.normalizeDate("")).isNull();
        assertThat(normalizer.normalizeDate("   ")).isNull();
    }

    // 실데이터 실측: MySQL 제로 날짜가 그대로 내려온다
    @Test
    void should_return_null_for_zero_date() {
        assertThat(normalizer.normalizeDate("0000-00-00")).isNull();
        assertThat(normalizer.normalizeDateTime("0000-00-00 00:00:00")).isNull();
    }

    @Test
    void should_convert_serial_with_time_fraction_to_datetime() {
        assertThat(normalizer.normalizeDateTime(45292.5)).isEqualTo("2024-01-01 12:00:00");
        assertThat(normalizer.normalizeDateTime("2025-01-15 10:30:00")).isEqualTo("2025-01-15 10:30:00");
        assertThat(normalizer.normalizeDateTime(null)).isNull();
    }

    @Test
    void should_strip_unit_from_amount() {
        assertThat(normalizer.normalizeAmount("1500점")).isEqualTo(1500);
        assertThat(normalizer.normalizeAmount("0점")).isEqualTo(0);
        assertThat(normalizer.normalizeAmount("0원")).isEqualTo(0);
        assertThat(normalizer.normalizeAmount("1,500점")).isEqualTo(1500);
        assertThat(normalizer.normalizeAmount(12500.0)).isEqualTo(12500);
        assertThat(normalizer.normalizeAmount(12500)).isEqualTo(12500);
        assertThat(normalizer.normalizeAmount(null)).isEqualTo(0);
    }

    @Test
    void should_normalize_approval_flag() {
        assertThat(normalizer.normalizeApproval("승인")).isEqualTo("y");
        assertThat(normalizer.normalizeApproval("미승인")).isEqualTo("n");
        assertThat(normalizer.normalizeApproval("y")).isEqualTo("y");
        assertThat(normalizer.normalizeApproval("n")).isEqualTo("n");
        assertThat(normalizer.normalizeApproval(null)).isEqualTo("n");
    }

    @Test
    void should_normalize_sex_flag_to_m_w_or_null() {
        assertThat(normalizer.normalizeSex("m")).isEqualTo("m");
        assertThat(normalizer.normalizeSex("W")).isEqualTo("w");
        assertThat(normalizer.normalizeSex("남자")).isNull();
        assertThat(normalizer.normalizeSex("")).isNull();
        assertThat(normalizer.normalizeSex(null)).isNull();
    }

    @Test
    void should_normalize_partner_registered_flag() {
        assertThat(normalizer.normalizePartnerRegistered("O")).isEqualTo("y");
        assertThat(normalizer.normalizePartnerRegistered("")).isEqualTo("n");
        assertThat(normalizer.normalizePartnerRegistered(null)).isEqualTo("n");
    }

    @Test
    void should_convert_numeric_cell_to_long() {
        assertThat(normalizer.normalizeLong(990954.0)).isEqualTo(990954L);
        assertThat(normalizer.normalizeLong(990954)).isEqualTo(990954L);
        assertThat(normalizer.normalizeLong("990954")).isEqualTo(990954L);
        assertThat(normalizer.normalizeLong(null)).isNull();
        assertThat(normalizer.normalizeLong("not-a-number")).isNull();
    }

    @Test
    void should_hash_cell_phone_without_hyphens() {
        GodoMemberEntity member = new GodoMemberEntity();
        normalizer.applyMemberFields(member, MemberRowFixture.listRow(), MemberUploadType.LIST);

        assertThat(member.getCellPhone()).isEqualTo("010-1234-5678");
        assertThat(member.getCellPhoneHash()).isEqualTo(hashUtil.hash("01012345678"));
    }

    @Test
    void should_set_name_hash_different_from_plaintext() {
        GodoMemberEntity member = new GodoMemberEntity();
        normalizer.applyMemberFields(member, MemberRowFixture.listRow(), MemberUploadType.LIST);

        assertThat(member.getMemName()).isEqualTo("홍길동");
        assertThat(member.getMemNameHash()).isEqualTo(hashUtil.hash("홍길동"));
        assertThat(member.getMemNameHash()).isNotEqualTo("홍길동");
    }

    @Test
    void should_set_null_name_and_hash_for_empty_cell() {
        List<Object> row = MemberRowFixture.listRow();
        row.set(10, null);
        GodoMemberEntity member = new GodoMemberEntity();
        normalizer.applyMemberFields(member, row, MemberUploadType.LIST);

        assertThat(member.getMemName()).isNull();
        assertThat(member.getMemNameHash()).isNull();
    }

    @Test
    void should_map_all_list_columns() {
        GodoMemberEntity member = new GodoMemberEntity();
        normalizer.applyMemberFields(member, MemberRowFixture.listRow(), MemberUploadType.LIST);

        assertThat(member.getEntryDt()).isEqualTo("2024-01-01 12:00:00");
        assertThat(member.getAppDt()).isEqualTo("2024-01-02 09:00:00");
        assertThat(member.getAppFl()).isEqualTo("y");
        assertThat(member.getEntryPath()).isEqualTo("pc");
        assertThat(member.getLastLoginDt()).isEqualTo("2026-06-01 10:00:00");
        assertThat(member.getMemberFl()).isEqualTo("business");
        assertThat(member.getGroupSno()).isEqualTo(9);
        assertThat(member.getMemId()).isEqualTo("user1");
        assertThat(member.getEmail()).isEqualTo("hong@example.com");
        assertThat(member.getMaillingFl()).isEqualTo("y");
        assertThat(member.getSmsFl()).isEqualTo("n");
        assertThat(member.getSaleAmount()).isEqualTo(12500);
        assertThat(member.getMileage()).isEqualTo(1500);
        assertThat(member.getDeposit()).isEqualTo(0);
        assertThat(member.getLoginCount()).isEqualTo(5);
        assertThat(member.getFax()).isEqualTo("02-000-0000");
        assertThat(member.getSexFl()).isEqualTo("m");
        assertThat(member.getBirthDt()).isEqualTo("1990-01-01");
        assertThat(member.getMarriFl()).isEqualTo("y");
        assertThat(member.getExpirationFl()).isEqualTo(1);
        assertThat(member.getEx1()).isEqualTo("식품");
    }

    @Test
    void should_apply_template_offset_for_member_fields() {
        List<Object> templateRow = MemberRowFixture.templateRow();
        Long memNo = normalizer.extractMemNo(templateRow, MemberUploadType.TEMPLATE);
        GodoMemberEntity member = new GodoMemberEntity();
        normalizer.applyMemberFields(member, templateRow, MemberUploadType.TEMPLATE);

        assertThat(memNo).isEqualTo(900101L);
        assertThat(member.getMemId()).isEqualTo("user1");
        assertThat(member.getMemName()).isEqualTo("홍길동");
    }

    @Test
    void should_map_business_fields_with_plaintext_business_no() {
        GodoMemberBusinessEntity business = new GodoMemberBusinessEntity();
        normalizer.applyBusinessFields(business, MemberRowFixture.listRow(), MemberUploadType.LIST);

        assertThat(business.getCompany()).isEqualTo("(주)테스트");
        assertThat(business.getBusinessNo()).isEqualTo("123-45-67890");
        assertThat(business.getCeoName()).isEqualTo("김대표");
        assertThat(business.getComAddress()).isEqualTo("06000 서울시 송파구 올림픽로 2");
        assertThat(business.getBizLicenseFile()).isNull();
    }

    @Test
    void should_map_partner_fields_from_template_columns() {
        MemberPartnerEntity partner = new MemberPartnerEntity();
        normalizer.applyPartnerFields(partner, MemberRowFixture.templateRow());

        assertThat(partner.getPartnerCode()).isEqualTo("990954");
        assertThat(partner.getPartnerName()).isEqualTo("(에쓰푸드몰)테스트");
        assertThat(partner.getTaxInvoiceFl()).isEqualTo("y");
        assertThat(partner.getMemberStatus()).isEqualTo("NORMAL");
    }

    @Test
    void should_map_partner_status_to_unregistered_when_template_column_not_registered() {
        List<Object> row = MemberRowFixture.templateRow();
        row.set(1, "X"); // 거래처등록 컬럼 값이 'O'가 아니면 미등록

        MemberPartnerEntity partner = new MemberPartnerEntity();
        normalizer.applyPartnerFields(partner, row);

        assertThat(partner.getMemberStatus()).isEqualTo("UNREGISTERED");
    }

    @Test
    void should_set_biz_license_file_from_template_column_7() {
        GodoMemberBusinessEntity business = new GodoMemberBusinessEntity();
        normalizer.applyBusinessFields(business, MemberRowFixture.templateRow(), MemberUploadType.TEMPLATE);

        assertThat(business.getBizLicenseFile()).isEqualTo("license.jpg");
    }

}
