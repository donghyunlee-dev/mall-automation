package com.sfood.automation.member.application.usecase;

import com.sfood.automation.member.application.MemberMasker;
import com.sfood.automation.member.application.dto.MemberListItemResponse;
import com.sfood.automation.member.application.dto.MemberPageResponse;
import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.domain.enums.MemberGroup;
import com.sfood.automation.member.domain.port.MemberListPage;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMemberListUseCase {

    private final MemberRepository memberRepository;
    private final MemberMasker masker;

    public GetMemberListUseCase(MemberRepository memberRepository, MemberMasker masker) {
        this.memberRepository = memberRepository;
        this.masker = masker;
    }

    @Transactional(readOnly = true)
    public MemberPageResponse<MemberListItemResponse> execute(MemberSearchCondition rawCondition) {
        MemberSearchCondition condition = rawCondition.normalized();
        MemberListPage page = memberRepository.findPage(condition);

        List<MemberListItemResponse> content = page.rows().stream()
            .map(row -> {
                GodoMemberEntity member = row.member();
                GodoMemberBusinessEntity biz = row.business();
                return new MemberListItemResponse(
                    member.getMemNo(),
                    member.getMemId(),
                    masker.maskName(member.getMemName()),
                    member.getGroupSno(),
                    MemberGroup.of(member.getGroupSno()).getLabel(),
                    masker.maskEmail(member.getEmail()),
                    masker.maskCellPhone(member.getCellPhone()),
                    biz != null ? biz.getCompany() : null,
                    biz != null ? biz.getBusinessNo() : null,
                    biz != null ? biz.getCeoName() : null,
                    biz != null ? biz.getServiceName() : null,
                    biz != null ? biz.getItemName() : null,
                    biz != null ? biz.getComAddress() : null,
                    row.partnerCode(),
                    row.partnerName(),
                    row.memberStatus(),
                    member.getEntryDt(),
                    member.getAppFl(),
                    biz != null ? biz.getDuplicateMemNo() : null);
            })
            .toList();

        int totalPages = (int) Math.ceil((double) page.totalElements() / condition.size());
        return new MemberPageResponse<>(
            content, condition.page(), condition.size(), page.totalElements(), totalPages);
    }
}
