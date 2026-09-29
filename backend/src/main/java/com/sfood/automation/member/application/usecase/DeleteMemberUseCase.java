package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.domain.port.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMemberUseCase {

    private final MemberRepository memberRepository;

    public DeleteMemberUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void execute(Long memNo) {
        memberRepository.findByMemNo(memNo).orElseThrow(MemberNotFoundException::new);
        memberRepository.deleteMember(memNo);
    }
}
