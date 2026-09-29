package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateResponse;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateMemberBusinessUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;

    public UpdateMemberBusinessUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberBusinessUpdateResponse execute(Long memNo, MemberBusinessUpdateRequest request, String loggedBy) {
        memberRepository.findByMemNo(memNo).orElseThrow(MemberNotFoundException::new);

        String now = LocalDateTime.now().format(DT_FMT);

        GodoMemberBusinessEntity biz = memberRepository.findBusinessByMemNo(memNo)
            .map(existing -> {
                GodoMemberBusinessLogEntity log = new GodoMemberBusinessLogEntity();
                log.setMemNo(existing.getMemNo());
                log.setCompany(existing.getCompany());
                log.setBusinessNo(existing.getBusinessNo());
                log.setCeoName(existing.getCeoName());
                log.setServiceName(existing.getServiceName());
                log.setServiceCode(existing.getServiceCode());
                log.setItemName(existing.getItemName());
                log.setItemCode(existing.getItemCode());
                log.setComAddress(existing.getComAddress());
                log.setBizLicenseFile(existing.getBizLicenseFile());
                log.setLoggedAt(now);
                log.setLoggedBy(loggedBy);
                log.setCreatedAt(now);
                memberRepository.saveBusinessLog(log);
                return existing;
            })
            .orElseGet(() -> {
                GodoMemberBusinessEntity created = new GodoMemberBusinessEntity();
                created.setMemNo(memNo);
                created.setCreatedAt(now);
                return created;
            });

        biz.setCompany(request.company());
        biz.setBusinessNo(request.businessNo());
        biz.setCeoName(request.ceoName());
        biz.setServiceName(request.serviceName());
        biz.setServiceCode(request.serviceCode());
        biz.setItemName(request.itemName());
        biz.setItemCode(request.itemCode());
        biz.setComAddress(request.comAddress());
        if (request.bizLicenseFile() != null) {
            biz.setBizLicenseFile(request.bizLicenseFile());
        }
        biz.setUpdatedAt(now);
        memberRepository.saveBusiness(biz);

        return new MemberBusinessUpdateResponse(
            biz.getCompany(),
            biz.getBusinessNo(),
            biz.getCeoName(),
            biz.getServiceName(),
            biz.getServiceCode(),
            biz.getItemName(),
            biz.getItemCode(),
            biz.getComAddress(),
            biz.getBizLicenseFile(),
            biz.getUpdatedAt()
        );
    }
}
