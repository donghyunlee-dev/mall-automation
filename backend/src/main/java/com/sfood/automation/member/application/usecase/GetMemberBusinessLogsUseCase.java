package com.sfood.automation.member.application.usecase;

import com.sfood.automation.member.application.dto.MemberBusinessLogListResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMemberBusinessLogsUseCase {

    private final MemberRepository memberRepository;

    public GetMemberBusinessLogsUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberBusinessLogListResponse execute(Long memNo) {
        return MemberBusinessLogListResponse.from(memberRepository.findBusinessLogsByMemNo(memNo));
    }
}
