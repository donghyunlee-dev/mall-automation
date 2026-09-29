package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.DuplicatePartnerCodeException;
import com.sfood.automation.common.exception.InvalidMemberStatusException;
import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateRequest;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class UpdateMemberPartnerUseCaseIntegrationTest {

    @Autowired
    private UploadMembersUseCase uploadMembersUseCase;

    @Autowired
    private UpdateMemberPartnerUseCase updateMemberPartnerUseCase;

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @Autowired
    private MemberPartnerLogJpaRepository partnerLogJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        partnerLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();

        // TEMPLATE 업로드 — partner 레코드도 생성되는 예외 케이스
        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.TEMPLATE, "template.xlsx", false,
                List.of(MemberRowFixture.templateRow())));

        // LIST 업로드 — 회원/사업자 정보만 생성
        List<Object> listOnlyRow = MemberRowFixture.listRow();
        listOnlyRow.set(0, 900200);
        listOnlyRow.set(8, "user2");   // mem_id UNIQUE 제약 — 900101("user1")과 충돌 방지
        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "list.xlsx", false, List.of(listOnlyRow)));

        // LIST 업로드 시 자동 생성된 900200 partner 레코드 제거
        // (이 클래스의 테스트는 900200에 partner 없는 상태에서 UpdateMemberPartnerUseCase를 호출하는 시나리오)
        memberPartnerJpaRepository.findByMemNo(900200L).ifPresent(memberPartnerJpaRepository::delete);
    }

    @AfterEach
    void tearDown() {
        partnerLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_insert_partner_record_when_not_exists() {
        MemberPartnerUpdateResponse response = updateMemberPartnerUseCase.execute(900200L,
            new MemberPartnerUpdateRequest("P-001", "신규명", "y", "NORMAL", null), null);

        assertThat(response.partnerCode()).isEqualTo("P-001");
        assertThat(response.partnerName()).isEqualTo("신규명");
        assertThat(response.taxInvoiceFl()).isEqualTo("y");
        assertThat(response.memberStatus()).isEqualTo("NORMAL");
        assertThat(response.updatedAt()).isNotNull();
        assertThat(memberPartnerJpaRepository.findByMemNo(900200L))
            .hasValueSatisfying(partner -> {
                assertThat(partner.getCreatedAt()).isNotNull();
                assertThat(partner.getUpdatedAt()).isEqualTo(response.updatedAt());
            });
    }

    @Test
    void should_update_existing_partner_record() {
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("P-001", "초기명", "n", null, null), null);

        MemberPartnerUpdateResponse response = updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("P-002", "수정명", "y", "NORMAL", null), null);

        assertThat(response.partnerCode()).isEqualTo("P-002");
        assertThat(response.partnerName()).isEqualTo("수정명");
        assertThat(response.taxInvoiceFl()).isEqualTo("y");
        assertThat(response.memberStatus()).isEqualTo("NORMAL");
        assertThat(response.updatedAt()).isNotNull();
        assertThat(memberPartnerJpaRepository.findAll()).hasSize(1);
    }

    @Test
    void should_clear_partner_code_when_null() {
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("990954", "(주)테스트", "n", null, null), null);

        MemberPartnerUpdateResponse response = updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest(null, null, "n", null, null), null);

        assertThat(response.partnerCode()).isNull();
        assertThat(response.partnerName()).isNull();
    }

    @Test
    void should_update_biz_license_file_when_business_exists() {
        MemberPartnerUpdateResponse response = updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("990954", "(주)테스트", "n", null, "biz_20260101.pdf"), null);

        assertThat(response.bizLicenseFile()).isEqualTo("biz_20260101.pdf");
        assertThat(godoMemberBusinessJpaRepository.findByMemNo(900101L))
            .hasValueSatisfying(biz -> assertThat(biz.getBizLicenseFile()).isEqualTo("biz_20260101.pdf"));
    }

    @Test
    void should_default_tax_invoice_fl_and_member_status_when_null() {
        MemberPartnerUpdateResponse response = updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest(null, null, null, null, null), null);

        assertThat(response.taxInvoiceFl()).isEqualTo("n");
        assertThat(response.memberStatus()).isEqualTo("NORMAL");
    }

    @Test
    void should_throw_400_for_invalid_member_status() {
        assertThatThrownBy(() -> updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest(null, null, null, "GARBAGE", null), null))
            .isInstanceOf(InvalidMemberStatusException.class);
    }

    @Test
    void should_throw_404_for_unknown_member() {
        assertThatThrownBy(() -> updateMemberPartnerUseCase.execute(99999999L,
            new MemberPartnerUpdateRequest(null, null, "n", null, null), null))
            .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void should_throw_409_when_partner_code_is_already_used_by_another_active_member() {
        // 두 번째 TEMPLATE 회원(900102) 업로드 후 API로 partner_code "12345" 등록
        List<Object> row2 = MemberRowFixture.templateRow();
        row2.set(8, 900102);      // list[0] = mem_no
        row2.set(16, "user-102"); // list[8] = mem_id
        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.TEMPLATE, "template2.xlsx", false, List.of(row2)));
        updateMemberPartnerUseCase.execute(900102L,
            new MemberPartnerUpdateRequest("12345", "등록명", "n", null, null), null);

        // 900101 회원이 "12345" 코드 사용 시도 → 900102가 이미 사용 중 → 409
        assertThatThrownBy(() -> updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("12345", "테스트", "n", null, null), null))
            .isInstanceOf(DuplicatePartnerCodeException.class);
    }

    @Test
    void should_allow_updating_to_same_partner_code_for_same_member() {
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("990954", "(주)테스트", "n", null, null), null);

        MemberPartnerUpdateResponse response = updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("990954", "(주)테스트2", "n", null, null), null);

        assertThat(response.partnerCode()).isEqualTo("990954");
    }

    @Test
    void should_save_partner_log_before_update() {
        // 900200L은 LIST 업로드로 생성 — partner 레코드 없음
        // 1차 execute: partner INSERT (로그 없음)
        updateMemberPartnerUseCase.execute(900200L,
            new MemberPartnerUpdateRequest("P-001", "초기명", "n", "NORMAL", null), "test@s-food.com");

        // 2차 execute: 기존 partner → 로그 저장 후 UPDATE
        updateMemberPartnerUseCase.execute(900200L,
            new MemberPartnerUpdateRequest("P-002", "수정명", "y", "INACTIVE", null), "test@s-food.com");

        List<MemberPartnerLogEntity> logs = partnerLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900200L);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getPartnerCode()).isEqualTo("P-001");
        assertThat(logs.get(0).getPartnerName()).isEqualTo("초기명");
        assertThat(logs.get(0).getMemberStatus()).isEqualTo("NORMAL");
        assertThat(logs.get(0).getLoggedBy()).isEqualTo("test@s-food.com");
    }

    @Test
    void should_not_save_log_on_first_insert() {
        // 900300L: 이 테스트 내에서만 생성하는 회원 — partner 레코드 없음 상태에서 테스트
        List<Object> newRow = MemberRowFixture.listRow();
        newRow.set(0, 900300);
        newRow.set(8, "user300");
        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "list300.xlsx", false, List.of(newRow)));
        // LIST 업로드 시 자동 생성된 partner 제거하여 "첫 등록" 시나리오 유지
        memberPartnerJpaRepository.findByMemNo(900300L).ifPresent(memberPartnerJpaRepository::delete);

        updateMemberPartnerUseCase.execute(900300L,
            new MemberPartnerUpdateRequest("P-001", "신규명", "y", "NORMAL", null), null);

        assertThat(partnerLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900300L)).isEmpty();
    }
}
