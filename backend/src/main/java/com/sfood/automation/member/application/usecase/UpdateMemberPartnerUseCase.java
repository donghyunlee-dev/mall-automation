package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.exception.DuplicatePartnerCodeException;
import com.sfood.automation.common.exception.InvalidMemberStatusException;
import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateRequest;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateResponse;
import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMemberPartnerUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;

    public UpdateMemberPartnerUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberPartnerUpdateResponse execute(Long memNo, MemberPartnerUpdateRequest request,
                                               String loggedBy) {
        memberRepository.findByMemNo(memNo).orElseThrow(MemberNotFoundException::new);

        if (request.memberStatus() != null && !MemberPartnerStatus.isValid(request.memberStatus())) {
            throw new InvalidMemberStatusException();
        }

        if (request.partnerCode() != null
                && memberRepository.existsActivePartnerCode(request.partnerCode(), memNo)) {
            throw new DuplicatePartnerCodeException();
        }

        String now = LocalDateTime.now().format(DT_FMT);

        MemberPartnerEntity partner = memberRepository.findPartnerByMemNo(memNo)
            .map(existing -> {
                MemberPartnerLogEntity log = new MemberPartnerLogEntity();
                log.setMemNo(existing.getMemNo());
                log.setPartnerCode(existing.getPartnerCode());
                log.setPartnerName(existing.getPartnerName());
                log.setTaxInvoiceFl(existing.getTaxInvoiceFl());
                log.setMemberStatus(existing.getMemberStatus());
                log.setLoggedAt(now);
                log.setLoggedBy(loggedBy);
                log.setCreatedAt(now);
                memberRepository.savePartnerLog(log);
                return existing;
            })
            .orElseGet(() -> {
                MemberPartnerEntity created = new MemberPartnerEntity();
                created.setMemNo(memNo);
                created.setCreatedAt(now);
                return created;
            });

        partner.setPartnerCode(request.partnerCode());
        partner.setPartnerName(request.partnerName());
        partner.setTaxInvoiceFl(request.taxInvoiceFl() != null ? request.taxInvoiceFl() : "n");
        partner.setMemberStatus(request.memberStatus() != null ? request.memberStatus() : MemberPartnerStatus.NORMAL.name());
        partner.setUpdatedAt(now);
        memberRepository.savePartner(partner);

        memberRepository.findBusinessByMemNo(memNo).ifPresent(biz -> {
            biz.setBizLicenseFile(request.bizLicenseFile());
            biz.setUpdatedAt(now);
            memberRepository.saveBusiness(biz);
        });

        return new MemberPartnerUpdateResponse(
            partner.getPartnerCode(),
            partner.getPartnerName(),
            partner.getTaxInvoiceFl(),
            partner.getMemberStatus(),
            request.bizLicenseFile(),
            now
        );
    }
}
