package com.sfood.automation.member.application.usecase;

import com.sfood.automation.member.application.dto.MemberPartnerLogListResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GetMemberPartnerLogsUseCase {

    private final MemberRepository memberRepository;

    public GetMemberPartnerLogsUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberPartnerLogListResponse execute(Long memNo) {
        return MemberPartnerLogListResponse.from(memberRepository.findPartnerLogsByMemNo(memNo));
    }
}
