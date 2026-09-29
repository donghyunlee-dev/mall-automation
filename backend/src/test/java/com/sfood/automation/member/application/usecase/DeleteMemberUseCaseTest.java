package com.sfood.automation.member.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteMemberUseCaseTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final DeleteMemberUseCase useCase = new DeleteMemberUseCase(memberRepository);

    @Test
    void should_call_deleteMember_when_member_exists() {
        GodoMemberEntity member = new GodoMemberEntity();
        member.setMemNo(1001L);
        when(memberRepository.findByMemNo(1001L)).thenReturn(Optional.of(member));

        useCase.execute(1001L);

        verify(memberRepository).deleteMember(1001L);
    }

    @Test
    void should_throw_MemberNotFoundException_when_member_not_exists() {
        when(memberRepository.findByMemNo(9999L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class, () -> useCase.execute(9999L));
    }
}
