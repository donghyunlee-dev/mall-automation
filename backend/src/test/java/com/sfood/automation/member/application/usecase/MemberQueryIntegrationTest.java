package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.MemberDetailResponse;
import com.sfood.automation.member.application.dto.MemberListItemResponse;
import com.sfood.automation.member.application.dto.MemberPageResponse;
import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.application.dto.MemberSearchCondition.GroupFilter;
import com.sfood.automation.member.application.dto.MemberSearchCondition.SearchType;
import com.sfood.automation.member.application.dto.MemberSummaryResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
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
class MemberQueryIntegrationTest {

    @Autowired
    private UploadMembersUseCase uploadMembersUseCase;

    @Autowired
    private GetMemberListUseCase getMemberListUseCase;

    @Autowired
    private GetMemberSummaryUseCase getMemberSummaryUseCase;

    @Autowired
    private GetMemberDetailUseCase getMemberDetailUseCase;

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();

        // B2B 사업자(900101, 홍길동) + 일반회원 2명(900102 승인 / 900103 미승인) 업로드
        List<Object> general1 = MemberRowFixture.listRow();
        general1.set(0, 900102);
        general1.set(7, 1);
        general1.set(8, "general-user");
        general1.set(10, "김일반");

        List<Object> general2 = MemberRowFixture.listRow();
        general2.set(0, 900103);
        general2.set(3, "미승인");
        general2.set(7, 1);
        general2.set(8, "pending-user");
        general2.set(10, "박대기");

        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "list.xlsx", false,
                List.of(MemberRowFixture.listRow(), general1, general2)));
    }

    @AfterEach
    void tearDown() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    private MemberSearchCondition condition(SearchType searchType, String keyword, GroupFilter group, String appFl) {
        return new MemberSearchCondition(searchType, keyword, group, appFl, null,
            null, null, null, null, null, null);
    }

    @Test
    void should_return_all_uploaded_members_with_masking() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, null, null));

        assertThat(page.totalElements()).isEqualTo(3);
        MemberListItemResponse b2b = page.content().stream()
            .filter(item -> item.memNo() == 900101L).findFirst().orElseThrow();
        assertThat(b2b.memName()).isEqualTo("홍*동");
        assertThat(b2b.email()).isEqualTo("ho***@example.com");
        assertThat(b2b.company()).isEqualTo("(주)테스트");
        assertThat(b2b.businessNo()).isEqualTo("123-45-67890");
        assertThat(b2b.ceoName()).isEqualTo("김대표");
        assertThat(b2b.serviceName()).isEqualTo("도매");
        assertThat(b2b.itemName()).isEqualTo("식품");
        assertThat(b2b.comAddress()).isEqualTo("06000 서울시 송파구 올림픽로 2");
        assertThat(b2b.groupName()).isEqualTo("B2B 거래처");
    }

    @Test
    void should_search_mem_id_partially() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(SearchType.MEM_ID, "general", null, null));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).memId()).isEqualTo("general-user");
    }

    @Test
    void should_search_name_by_exact_hash_match() {
        MemberPageResponse<MemberListItemResponse> exact =
            getMemberListUseCase.execute(condition(SearchType.NAME, "김일반", null, null));
        assertThat(exact.totalElements()).isEqualTo(1);
        assertThat(exact.content().get(0).memNo()).isEqualTo(900102L);

        MemberPageResponse<MemberListItemResponse> partial =
            getMemberListUseCase.execute(condition(SearchType.NAME, "김일", null, null));
        assertThat(partial.totalElements()).isEqualTo(0);
    }

    private void savePartner(long memNo, String partnerCode, String partnerName) {
        MemberPartnerEntity partner = memberPartnerJpaRepository.findByMemNo(memNo)
            .orElseGet(MemberPartnerEntity::new);
        partner.setMemNo(memNo);
        partner.setMemberStatus(MemberPartnerStatus.NORMAL.name());
        partner.setPartnerCode(partnerCode);
        partner.setPartnerName(partnerName);
        if (partner.getCreatedAt() == null) {
            partner.setCreatedAt("2026-06-22 00:00:00");
        }
        partner.setUpdatedAt("2026-06-22 00:00:00");
        memberPartnerJpaRepository.save(partner);
    }

    @Test
    void should_search_partner_code_partially() {
        savePartner(900101L, "P12345", "홍길동상회");

        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(SearchType.PARTNER_CODE, "P123", null, null));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).memNo()).isEqualTo(900101L);
        assertThat(page.content().get(0).partnerCode()).isEqualTo("P12345");
    }

    @Test
    void should_search_partner_name_partially() {
        savePartner(900101L, "P12345", "홍길동상회");

        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(SearchType.PARTNER_NAME, "홍길동", null, null));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).partnerName()).isEqualTo("홍길동상회");
    }

    @Test
    void should_return_empty_when_partner_keyword_has_no_match() {
        savePartner(900101L, "P12345", "홍길동상회");

        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(SearchType.PARTNER_CODE, "ZZZ", null, null));

        assertThat(page.totalElements()).isEqualTo(0);
    }

    @Test
    void should_search_cell_phone_ignoring_hyphens() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(SearchType.CELL_PHONE, "01012345678", null, null));

        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void should_filter_by_b2b_group() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, GroupFilter.B2B, null));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).groupSno()).isEqualTo(9);
    }

    @Test
    void should_filter_by_general_group_excludes_skilled_and_b2b() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, GroupFilter.GENERAL, null));

        // setUp: 일반(1) 2명만 — 기능장(8)·B2B(9) 제외
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content().stream().map(MemberListItemResponse::groupSno).toList())
            .containsOnly(1);
    }

    @Test
    void should_filter_by_skilled_group() {
        // 기능장회원(group_sno=8) 추가 업로드
        List<Object> skilled = MemberRowFixture.listRow();
        skilled.set(0, 900104);
        skilled.set(7, 8);
        skilled.set(8, "skilled-user");
        uploadMembersUseCase.execute(
            new MemberUploadRequest(MemberUploadType.LIST, "list.xlsx", false, List.of(skilled)));

        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, GroupFilter.SKILLED, null));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).groupSno()).isEqualTo(8);
    }

    @Test
    void should_not_include_b2b_in_business_filter() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, GroupFilter.BUSINESS, null));

        // setUp에 사업자(2,3,5,6)가 없으므로 0건, B2B(9)는 포함되지 않아야 함
        assertThat(page.totalElements()).isEqualTo(0);
    }

    @Test
    void should_filter_by_pending_approval() {
        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, null, "n"));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).memId()).isEqualTo("pending-user");
    }

    @Test
    void should_paginate_with_total_elements() {
        MemberPageResponse<MemberListItemResponse> page = getMemberListUseCase.execute(
            new MemberSearchCondition(null, null, null, null, null, null, null, "mem_no", "asc", 0, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content().get(0).memNo()).isEqualTo(900101L);
    }

    @Test
    void should_fall_back_to_mem_no_desc_for_disallowed_sort_key() {
        MemberPageResponse<MemberListItemResponse> page = getMemberListUseCase.execute(
            new MemberSearchCondition(null, null, null, null, null, null, null, "mem_name", null, 0, 20));

        assertThat(page.content().get(0).memNo()).isEqualTo(900103L);
    }

    @Test
    void should_return_summary_counts() {
        MemberSummaryResponse summary = getMemberSummaryUseCase.execute();

        assertThat(summary.totalCount()).isEqualTo(3);
        assertThat(summary.businessCount()).isEqualTo(0); // setUp에 사업자(2,3,5,6)는 없음, B2B(9) 별도 카운트
        assertThat(summary.b2bCount()).isEqualTo(1);
        assertThat(summary.pendingApprovalCount()).isEqualTo(1);
        assertThat(summary.lastSyncedAt()).isNotNull();
    }

    @Test
    void should_return_decrypted_detail_with_business_and_no_masking() {
        MemberDetailResponse detail = getMemberDetailUseCase.execute(900101L);

        assertThat(detail.memName()).isEqualTo("홍길동");
        assertThat(detail.email()).isEqualTo("hong@example.com");
        assertThat(detail.cellPhone()).isEqualTo("010-1234-5678");
        assertThat(detail.address()).isEqualTo("06000 서울시 강남구 테헤란로 1");
        assertThat(detail.business()).isNotNull();
        assertThat(detail.business().businessNo()).isEqualTo("123-45-67890");
        assertThat(detail.business().ceoName()).isEqualTo("김대표");
        // LIST 업로드 시 자동 생성된 partner 레코드가 존재하나 미등록 상태(memberStatus=UNREGISTERED)
        assertThat(detail.partner()).isNotNull();
        assertThat(detail.partner().memberStatus()).isEqualTo("UNREGISTERED");
    }

    @Test
    void should_exclude_soft_deleted_members() {
        var member = godoMemberJpaRepository.findByMemNo(900103L).orElseThrow();
        member.setDeletedFl("y");
        godoMemberJpaRepository.save(member);

        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, null, null));
        MemberSummaryResponse summary = getMemberSummaryUseCase.execute();

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(summary.totalCount()).isEqualTo(2);
    }

    @Test
    void should_filter_member_status_normal() {
        savePartner(900101L, "P001", "홍길동상회");  // memberStatus = NORMAL

        MemberSearchCondition cond = new MemberSearchCondition(
            null, null, null, null, "NORMAL", null, null, null, null, null, null);
        MemberPageResponse<MemberListItemResponse> page = getMemberListUseCase.execute(cond);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).memNo()).isEqualTo(900101L);
        assertThat(page.content().get(0).memberStatus()).isEqualTo("NORMAL");
    }

    @Test
    void should_filter_member_status_unregistered() {
        // 900101: memberStatus='NORMAL' (등록)
        savePartner(900101L, "P001", "홍길동상회");
        // 900102, 900103: LIST 업로드로 자동 생성된 미등록(UNREGISTERED) 상태 유지

        MemberSearchCondition cond = new MemberSearchCondition(
            null, null, null, null, "UNREGISTERED", null, null, null, null, null, null);
        MemberPageResponse<MemberListItemResponse> page = getMemberListUseCase.execute(cond);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content().stream().map(MemberListItemResponse::memNo).toList())
            .containsExactlyInAnyOrder(900102L, 900103L);
    }

    @Test
    void should_not_filter_member_status_when_not_specified() {
        savePartner(900101L, "P001", "홍길동상회");

        MemberPageResponse<MemberListItemResponse> page =
            getMemberListUseCase.execute(condition(null, null, null, null));

        assertThat(page.totalElements()).isEqualTo(3);
    }
}
