package com.sfood.automation.member.application.usecase;

import com.sfood.automation.member.application.dto.MemberSummaryResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.domain.port.MemberSummaryCounts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMemberSummaryUseCase {

    private final MemberRepository memberRepository;

    public GetMemberSummaryUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public MemberSummaryResponse execute() {
        MemberSummaryCounts counts = memberRepository.findSummary();
        return new MemberSummaryResponse(
            counts.totalCount(),
            counts.businessCount(),
            counts.b2bCount(),
            counts.pendingApprovalCount(),
            counts.lastSyncedAt());
    }
}
