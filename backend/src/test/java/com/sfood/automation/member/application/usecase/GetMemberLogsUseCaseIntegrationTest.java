package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.MemberBusinessLogListResponse;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.application.dto.MemberPartnerLogListResponse;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateRequest;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
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
class GetMemberLogsUseCaseIntegrationTest {

    @Autowired private UploadMembersUseCase uploadMembersUseCase;
    @Autowired private UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
    @Autowired private UpdateMemberPartnerUseCase updateMemberPartnerUseCase;
    @Autowired private GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase;
    @Autowired private GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase;
    @Autowired private GodoMemberJpaRepository godoMemberJpaRepository;
    @Autowired private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;
    @Autowired private GodoMemberBusinessLogJpaRepository businessLogJpaRepository;
    @Autowired private MemberPartnerJpaRepository memberPartnerJpaRepository;
    @Autowired private MemberPartnerLogJpaRepository partnerLogJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        businessLogJpaRepository.deleteAll();
        partnerLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();

        uploadMembersUseCase.execute(new MemberUploadRequest(
            MemberUploadType.LIST, "list.xlsx", false, List.of(MemberRowFixture.listRow())));

        // LIST 업로드 시 자동 생성된 partner 레코드 제거
        // (이 클래스의 테스트는 partner 없는 상태에서 UpdateMemberPartnerUseCase를 호출하는 시나리오)
        memberPartnerJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        businessLogJpaRepository.deleteAll();
        partnerLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_return_business_logs_in_descending_order() {
        updateMemberBusinessUseCase.execute(900101L,
            new MemberBusinessUpdateRequest("1차수정", null, null, null, null, null, null, null, null), "admin@s-food.com");
        updateMemberBusinessUseCase.execute(900101L,
            new MemberBusinessUpdateRequest("2차수정", null, null, null, null, null, null, null, null), "admin@s-food.com");

        MemberBusinessLogListResponse response = getMemberBusinessLogsUseCase.execute(900101L);

        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(0).company()).isEqualTo("1차수정"); // 최신순 → 2차 직전 = 1차수정 상태
    }

    @Test
    void should_return_partner_logs_in_descending_order() {
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("P-001", "1차", "n", "NORMAL", null), "admin@s-food.com");
        updateMemberPartnerUseCase.execute(900101L,
            new MemberPartnerUpdateRequest("P-002", "2차", "y", "INACTIVE", null), "admin@s-food.com");

        MemberPartnerLogListResponse response = getMemberPartnerLogsUseCase.execute(900101L);

        assertThat(response.logs()).hasSize(1);
        assertThat(response.logs().get(0).partnerCode()).isEqualTo("P-001");
    }

    @Test
    void should_return_empty_when_no_logs() {
        MemberBusinessLogListResponse response = getMemberBusinessLogsUseCase.execute(900101L);
        assertThat(response.logs()).isEmpty();
    }
}
