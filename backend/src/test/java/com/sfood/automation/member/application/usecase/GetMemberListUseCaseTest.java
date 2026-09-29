package com.sfood.automation.member.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sfood.automation.member.application.MemberMasker;
import com.sfood.automation.member.application.dto.MemberListItemResponse;
import com.sfood.automation.member.application.dto.MemberPageResponse;
import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.domain.port.MemberListPage;
import com.sfood.automation.member.domain.port.MemberListRow;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetMemberListUseCaseTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberMasker masker = new MemberMasker();
    private final GetMemberListUseCase useCase = new GetMemberListUseCase(memberRepository, masker);

    @Test
    void should_return_masked_cell_phone_in_list() {
        GodoMemberEntity member = memberWithCellPhone("010-1234-5678");
        when(memberRepository.findPage(any())).thenReturn(
            new MemberListPage(List.of(new MemberListRow(member, null, null, null, null)), 1L));

        MemberPageResponse<MemberListItemResponse> result = useCase.execute(defaultCondition());

        assertThat(result.content().get(0).cellPhone()).isEqualTo("010-****-5678");
    }

    @Test
    void should_return_null_cell_phone_when_not_set() {
        GodoMemberEntity member = memberWithCellPhone(null);
        when(memberRepository.findPage(any())).thenReturn(
            new MemberListPage(List.of(new MemberListRow(member, null, null, null, null)), 1L));

        MemberPageResponse<MemberListItemResponse> result = useCase.execute(defaultCondition());

        assertThat(result.content().get(0).cellPhone()).isNull();
    }

    private GodoMemberEntity memberWithCellPhone(String cellPhone) {
        GodoMemberEntity member = new GodoMemberEntity();
        member.setMemNo(1L);
        member.setGroupSno(1);
        member.setCellPhone(cellPhone);
        return member;
    }

    private MemberSearchCondition defaultCondition() {
        return new MemberSearchCondition(null, null, null, null, null, null, null, null, null, 0, 20);
    }
}
