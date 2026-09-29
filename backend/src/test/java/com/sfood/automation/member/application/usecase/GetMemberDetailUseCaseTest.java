package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sfood.automation.member.application.dto.MemberDetailResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetMemberDetailUseCaseTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final GetMemberDetailUseCase useCase = new GetMemberDetailUseCase(memberRepository);

    @Test
    void should_return_business_when_general_grade_but_business_record_exists() {
        GodoMemberEntity member = new GodoMemberEntity();
        member.setMemNo(900101L);
        member.setGroupSno(1); // 미승인 수집 시점: ERP 승인 전이라 아직 일반회원 등급
        when(memberRepository.findByMemNo(900101L)).thenReturn(Optional.of(member));

        GodoMemberBusinessEntity business = new GodoMemberBusinessEntity();
        business.setCompany("(주)테스트");
        business.setBusinessNo("123-45-67890");
        when(memberRepository.findBusinessByMemNo(900101L)).thenReturn(Optional.of(business));
        when(memberRepository.findPartnerByMemNo(900101L)).thenReturn(Optional.empty());

        MemberDetailResponse result = useCase.execute(900101L);

        assertThat(result.business()).isNotNull();
        assertThat(result.business().company()).isEqualTo("(주)테스트");
    }

    @Test
    void should_return_null_business_when_no_business_record() {
        GodoMemberEntity member = new GodoMemberEntity();
        member.setMemNo(900102L);
        member.setGroupSno(1);
        when(memberRepository.findByMemNo(900102L)).thenReturn(Optional.of(member));
        when(memberRepository.findBusinessByMemNo(900102L)).thenReturn(Optional.empty());
        when(memberRepository.findPartnerByMemNo(900102L)).thenReturn(Optional.empty());

        MemberDetailResponse result = useCase.execute(900102L);

        assertThat(result.business()).isNull();
    }
}
