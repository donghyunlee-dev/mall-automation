package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
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
class UpdateMemberBusinessUseCaseIntegrationTest {

    @Autowired private UploadMembersUseCase uploadMembersUseCase;
    @Autowired private UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
    @Autowired private GodoMemberJpaRepository godoMemberJpaRepository;
    @Autowired private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;
    @Autowired private GodoMemberBusinessLogJpaRepository businessLogJpaRepository;
    @Autowired private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        uploadMembersUseCase.execute(new MemberUploadRequest(
            MemberUploadType.LIST, "list.xlsx", false, List.of(MemberRowFixture.listRow())));
    }

    @AfterEach
    void tearDown() {
        businessLogJpaRepository.deleteAll();
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_update_business_info_and_save_log() {
        MemberBusinessUpdateRequest request = new MemberBusinessUpdateRequest(
            "수정상호", "222-33-44444", "수정대표", "도매", null, "식품류", null, "서울시 수정구", null);

        MemberBusinessUpdateResponse response =
            updateMemberBusinessUseCase.execute(900101L, request, "admin@s-food.com");

        // 응답에 저장된 값이 반환되는지 확인
        assertThat(response.company()).isEqualTo("수정상호");
        assertThat(response.businessNo()).isEqualTo("222-33-44444");
        assertThat(response.ceoName()).isEqualTo("수정대표");
        assertThat(response.updatedAt()).isNotNull();

        // 사업자 정보 업데이트 확인
        var biz = godoMemberBusinessJpaRepository.findByMemNo(900101L).orElseThrow();
        assertThat(biz.getCompany()).isEqualTo("수정상호");
        assertThat(biz.getBusinessNo()).isEqualTo("222-33-44444");
        assertThat(biz.getCeoName()).isEqualTo("수정대표");

        // 로그 저장 확인
        var logs = businessLogJpaRepository.findByMemNoOrderByLoggedAtDesc(900101L);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCompany()).isEqualTo("(주)테스트"); // 수정 전 원래 값
        assertThat(logs.get(0).getLoggedBy()).isEqualTo("admin@s-food.com");
    }

    @Test
    void should_throw_404_for_unknown_member() {
        assertThatThrownBy(() -> updateMemberBusinessUseCase.execute(99999L,
            new MemberBusinessUpdateRequest(null, null, null, null, null, null, null, null, null), null))
            .isInstanceOf(MemberNotFoundException.class);
    }
}
