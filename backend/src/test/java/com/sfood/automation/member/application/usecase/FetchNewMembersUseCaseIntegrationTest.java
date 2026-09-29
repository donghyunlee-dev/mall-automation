package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FetchNewMembersUseCaseIntegrationTest {

    @Autowired
    private FetchNewMembersUseCase fetchNewMembersUseCase;

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

    @AfterEach
    void tearDown() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    /**
     * CrawledMember 필드 순서:
     * memNo, memId, groupSno, memberFl, appFl, appDt, entryDt, entryPath, lastLoginDt,
     * memName, nickName, email, cellPhone, phone, address, sexFl, birthDt,
     * marriFl, marriDate, maillingFl, smsFl, job, interest, recommId,
     * saleAmount, mileage, deposit, loginCount, fax, expirationFl, memo,
     * ex1, ex2, ex3,
     * company, businessNo, ceoName, service, item, comAddress
     */
    private CrawledMember generalMemberWithBiz(long memNo, String memId, String company, String bizNo) {
        return new CrawledMember(
            memNo, memId, 1, "normal", "y", "2024-01-01", "2024-01-01",
            "pc", "2024-06-01", "테스트회원", "테스터", "test@example.com",
            "01012345678", null, "서울시", "m", "19900101",
            "n", null, "y", "y", null, null, null,
            0.0, 0.0, 0.0, 0, null, 1, null, null, null, null,
            company, bizNo, "대표자", "식품", "가공식품", "서울시"
        );
    }

    @Test
    void should_save_biz_info_for_general_member_when_biz_fields_present() {
        // groupSno=1(일반회원)이지만 company/bizNo 있으면 godoMemberBusiness 저장됨
        CrawledMember member = generalMemberWithBiz(8001L, "general-biz-user", "달콤가든", "111-22-33333");

        fetchNewMembersUseCase.execute(List.of(member));

        var business = godoMemberBusinessJpaRepository.findByMemNo(8001L);
        assertThat(business).isPresent();
        assertThat(business.get().getCompany()).isEqualTo("달콤가든");
        assertThat(business.get().getBusinessNo()).isEqualTo("111-22-33333");
    }

    @Test
    void should_auto_create_partner_with_code_and_name() {
        // company="달콤가든" → partnerName="(에쓰푸드몰)달콤가든", partnerCode not null, memberStatus="UNREGISTERED"
        // 신규 회원 등록 시 거래처는 미등록(UNREGISTERED) 상태로 생성됨. ERP에서 수동으로 등록 후 상태 변경
        CrawledMember member = generalMemberWithBiz(8002L, "partner-auto-user", "달콤가든", "222-33-44444");

        fetchNewMembersUseCase.execute(List.of(member));

        var partner = memberPartnerJpaRepository.findByMemNo(8002L);
        assertThat(partner).isPresent();
        assertThat(partner.get().getPartnerName()).isEqualTo("(에쓰푸드몰)달콤가든");
        assertThat(partner.get().getPartnerCode()).isNotNull();
        assertThat(partner.get().getMemberStatus()).isEqualTo("UNREGISTERED");
    }

    @Test
    void should_auto_create_partner_code_max_plus_one() {
        // 첫번째 회원 코드 + 1 = 두번째 회원 코드
        CrawledMember member1 = generalMemberWithBiz(8003L, "user-code-1", "회사A", "333-44-55555");
        fetchNewMembersUseCase.execute(List.of(member1));
        String code1 = memberPartnerJpaRepository.findByMemNo(8003L).orElseThrow().getPartnerCode();

        CrawledMember member2 = generalMemberWithBiz(8004L, "user-code-2", "회사B", "444-55-66666");
        fetchNewMembersUseCase.execute(List.of(member2));
        String code2 = memberPartnerJpaRepository.findByMemNo(8004L).orElseThrow().getPartnerCode();

        assertThat(Integer.parseInt(code2)).isEqualTo(Integer.parseInt(code1) + 1);
    }

    @Test
    void should_set_duplicate_mem_no_when_same_biz_no_exists() {
        // 같은 bizNo 2명 → 각각 상대방 memNo를 duplicateMemNo에 저장
        String sharedBizNo = "555-66-77777";
        CrawledMember member1 = generalMemberWithBiz(8005L, "dup-biz-user1", "회사X", sharedBizNo);
        CrawledMember member2 = generalMemberWithBiz(8006L, "dup-biz-user2", "회사X", sharedBizNo);

        fetchNewMembersUseCase.execute(List.of(member1, member2));

        var biz1 = godoMemberBusinessJpaRepository.findByMemNo(8005L).orElseThrow();
        var biz2 = godoMemberBusinessJpaRepository.findByMemNo(8006L).orElseThrow();
        assertThat(biz1.getDuplicateMemNo()).contains("8006");
        assertThat(biz2.getDuplicateMemNo()).contains("8005");
    }

    @Test
    void should_not_create_partner_if_already_exists() {
        // 같은 memNo로 두 번 fetch → partner 1개만 생성
        CrawledMember member = generalMemberWithBiz(8007L, "idempotent-user", "회사Y", "666-77-88888");

        fetchNewMembersUseCase.execute(List.of(member));
        fetchNewMembersUseCase.execute(List.of(member));

        long count = memberPartnerJpaRepository.findAll().stream()
            .filter(p -> p.getMemNo().equals(8007L))
            .count();
        assertThat(count).isEqualTo(1);
    }
}
