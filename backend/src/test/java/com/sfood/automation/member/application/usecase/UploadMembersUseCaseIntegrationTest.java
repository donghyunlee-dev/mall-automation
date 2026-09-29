package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class UploadMembersUseCaseIntegrationTest {

    @Autowired
    private UploadMembersUseCase uploadMembersUseCase;

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    private MemberUploadCompleteResponse executeList(List<List<Object>> rows) {
        return uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "list.xlsx", false, rows));
    }

    private MemberUploadCompleteResponse executeList(List<List<Object>> rows, boolean applyDeletion) {
        return uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "list.xlsx", applyDeletion, rows));
    }

    @Test
    void should_upsert_list_row_idempotently() {
        MemberUploadCompleteResponse first = executeList(List.of(MemberRowFixture.listRow()));
        assertThat(first.totalInserted()).isEqualTo(1);
        assertThat(first.totalUpdated()).isEqualTo(0);

        MemberUploadCompleteResponse second = executeList(List.of(MemberRowFixture.listRow()));
        assertThat(second.totalInserted()).isEqualTo(0);
        assertThat(second.totalUpdated()).isEqualTo(1);

        assertThat(godoMemberJpaRepository.findAll()).hasSize(1);
        GodoMemberEntity saved = godoMemberJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(saved.getMemName()).isEqualTo("홍길동");

        String storedMemName = jdbcTemplate.queryForObject(
            "SELECT mem_name FROM godo_member WHERE mem_no = 900101", String.class);
        assertThat(storedMemName).isNotEqualTo("홍길동");
    }

    @Test
    void should_isolate_row_without_mem_no_and_process_the_rest() {
        List<Object> rowWithoutMemNo = MemberRowFixture.listRow();
        rowWithoutMemNo.set(0, null);

        MemberUploadCompleteResponse response = executeList(List.of(rowWithoutMemNo, MemberRowFixture.listRow()));

        assertThat(response.totalInserted()).isEqualTo(1);
        assertThat(response.totalFailed()).isEqualTo(1);
        assertThat(response.failedRows()).hasSize(1);
        assertThat(response.failedRows().get(0).reason()).contains("회원번호");
        assertThat(godoMemberJpaRepository.findAll()).hasSize(1);
    }

    @Test
    void should_create_business_row_for_business_group_member() {
        executeList(List.of(MemberRowFixture.listRow()));

        var business = godoMemberBusinessJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(business.getBusinessNo()).isEqualTo("123-45-67890");
        assertThat(business.getCeoName()).isEqualTo("김대표");
    }

    @Test
    void should_preserve_partner_data_on_list_upload() {
        saveActiveMember(900101L, "user1");
        MemberPartnerEntity partner = new MemberPartnerEntity();
        partner.setMemNo(900101L);
        partner.setPartnerCode("990954");
        partner.setPartnerName("(에쓰푸드몰)테스트");
        partner.setTaxInvoiceFl("y");
        partner.setCreatedAt("2026-06-12T00:00:00Z");
        partner.setUpdatedAt("2026-06-12T00:00:00Z");
        memberPartnerJpaRepository.save(partner);

        executeList(List.of(MemberRowFixture.listRow()));

        var preserved = memberPartnerJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(preserved.getPartnerCode()).isEqualTo("990954");
        assertThat(preserved.getUpdatedAt()).isEqualTo("2026-06-12T00:00:00Z");
    }

    @Test
    void should_upsert_partner_on_template_upload() {
        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.TEMPLATE, "template.xlsx", false,
                List.of(MemberRowFixture.templateRow())));

        var partner = memberPartnerJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(partner.getMemberStatus()).isEqualTo("NORMAL");
        assertThat(partner.getPartnerCode()).isEqualTo("990954");

        var business = godoMemberBusinessJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(business.getBizLicenseFile()).isEqualTo("license.jpg");
    }

    @Test
    void should_soft_delete_not_received_members_under_guard_ratio() {
        for (int i = 0; i < 20; i++) {
            saveActiveMember(910000L + i, "existing-" + i);
        }
        List<List<Object>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 19; i++) {
            List<Object> row = MemberRowFixture.listRow();
            row.set(0, 910000 + i);
            row.set(8, "existing-" + i);
            rows.add(row);
        }

        MemberUploadCompleteResponse response = executeList(rows, true);

        assertThat(response.deletionSkipped()).isFalse();
        assertThat(response.totalDeleted()).isEqualTo(1);
        GodoMemberEntity deleted = godoMemberJpaRepository.findByMemNo(910019L).orElseThrow();
        assertThat(deleted.getDeletedFl()).isEqualTo("y");
    }

    @Test
    void should_skip_deletion_when_over_10_percent() {
        for (int i = 0; i < 20; i++) {
            saveActiveMember(920000L + i, "bulk-" + i);
        }
        List<Object> row = MemberRowFixture.listRow();
        row.set(0, 920000);
        row.set(8, "bulk-0");

        MemberUploadCompleteResponse response = executeList(List.of(row), true);

        assertThat(response.deletionSkipped()).isTrue();
        assertThat(response.deletionSkippedReason()).contains("10%");
        assertThat(response.totalDeleted()).isEqualTo(0);
        assertThat(godoMemberJpaRepository.findAll())
            .allSatisfy(member -> assertThat(member.getDeletedFl()).isEqualTo("n"));
    }

    private void saveActiveMember(Long memNo, String memId) {
        GodoMemberEntity member = new GodoMemberEntity();
        member.setMemNo(memNo);
        member.setMemId(memId);
        member.setSyncedAt("2026-06-12T00:00:00Z");
        member.setCreatedAt("2026-06-12T00:00:00Z");
        member.setUpdatedAt("2026-06-12T00:00:00Z");
        godoMemberJpaRepository.save(member);
    }
}
