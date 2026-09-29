package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberBusinessDetailResponse;
import com.sfood.automation.member.application.dto.MemberDetailResponse;
import com.sfood.automation.member.application.dto.MemberPartnerDetailResponse;
import com.sfood.automation.member.domain.enums.MemberGroup;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMemberDetailUseCase {

    private final MemberRepository memberRepository;

    public GetMemberDetailUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse execute(Long memNo) {
        GodoMemberEntity member = memberRepository.findByMemNo(memNo)
            .orElseThrow(MemberNotFoundException::new);

        MemberBusinessDetailResponse business = memberRepository.findBusinessByMemNo(memNo)
            .map(entity -> new MemberBusinessDetailResponse(
                entity.getCompany(),
                entity.getBusinessNo(),
                entity.getCeoName(),
                entity.getServiceName(),
                entity.getServiceCode(),
                entity.getItemName(),
                entity.getItemCode(),
                entity.getComAddress(),
                entity.getBizLicenseFile(),
                entity.getDuplicateMemNo()))
            .orElse(null);

        MemberPartnerDetailResponse partner = memberRepository.findPartnerByMemNo(memNo)
            .map(entity -> new MemberPartnerDetailResponse(
                entity.getPartnerCode(),
                entity.getPartnerName(),
                entity.getTaxInvoiceFl(),
                entity.getMemberStatus(),
                entity.getUpdatedAt()))
            .orElse(null);

        return new MemberDetailResponse(
            member.getMemNo(),
            member.getMemId(),
            member.getMemName(),
            member.getGroupSno(),
            MemberGroup.of(member.getGroupSno()).getLabel(),
            member.getAppFl(),
            member.getAppDt(),
            member.getEmail(),
            member.getCellPhone(),
            member.getPhone(),
            member.getAddress(),
            member.getEntryDt(),
            member.getLastLoginDt(),
            member.getEntryPath(),
            member.getMaillingFl(),
            member.getSmsFl(),
            member.getNickName(),
            member.getSexFl(),
            member.getBirthDt(),
            member.getMarriFl(),
            member.getMarriDate(),
            member.getJob(),
            member.getInterest(),
            member.getLoginCount(),
            member.getSaleAmount(),
            member.getMileage(),
            member.getDeposit(),
            member.getRecommId(),
            member.getEx1(),
            member.getEx2(),
            member.getEx3(),
            member.getEx4(),
            member.getEx5(),
            member.getEx6(),
            member.getMemo(),
            business,
            partner);
    }
}
