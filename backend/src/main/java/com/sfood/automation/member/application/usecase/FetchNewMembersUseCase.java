package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common.security.HashUtil;
import com.sfood.automation.common_code.application.usecase.CommonCodeMappingService;
import com.sfood.automation.member.application.dto.CrawledMember;
import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FetchNewMembersUseCase {

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;
    private final HashUtil hashUtil;
    private final CommonCodeMappingService commonCodeMappingService;

    public FetchNewMembersUseCase(MemberRepository memberRepository,
                                   HashUtil hashUtil,
                                   CommonCodeMappingService commonCodeMappingService) {
        this.memberRepository = memberRepository;
        this.hashUtil = hashUtil;
        this.commonCodeMappingService = commonCodeMappingService;
    }

    @Transactional
    public String execute(List<CrawledMember> crawledMembers) {
        if (crawledMembers.isEmpty()) {
            return "신규 0건, 업데이트 0건";
        }

        String now = LocalDateTime.now().format(DT_FMT);
        List<Long> allMemNos = crawledMembers.stream().map(CrawledMember::memNo).toList();
        Set<Long> existingNos = new HashSet<>(memberRepository.findExistingMemNos(allMemNos));

        List<GodoMemberEntity> toInsert = crawledMembers.stream()
            .filter(m -> !existingNos.contains(m.memNo()))
            .map(m -> toEntity(m, now, true))
            .toList();

        List<GodoMemberEntity> toUpdate = crawledMembers.stream()
            .filter(m -> existingNos.contains(m.memNo()))
            .map(m -> toEntity(m, now, false))
            .toList();

        if (!toInsert.isEmpty()) {
            memberRepository.bulkInsertMembers(toInsert);
        }
        if (!toUpdate.isEmpty()) {
            memberRepository.bulkUpdateMembers(toUpdate);
        }

        // 사업자 정보: hasBusiness() 조건 제거 → 사업자 필드 유무로 판단
        List<CrawledMember> bizMembers = crawledMembers.stream()
            .filter(m -> m.company() != null || m.businessNo() != null)
            .toList();

        if (!bizMembers.isEmpty()) {
            List<Long> bizNos = bizMembers.stream().map(CrawledMember::memNo).toList();
            Set<Long> existingBizNos = new HashSet<>(memberRepository.findExistingBusinessNos(bizNos));

            List<GodoMemberBusinessEntity> bizInsert = bizMembers.stream()
                .filter(m -> !existingBizNos.contains(m.memNo()))
                .map(m -> toBizEntity(m, now, true))
                .toList();

            List<GodoMemberBusinessEntity> bizUpdate = bizMembers.stream()
                .filter(m -> existingBizNos.contains(m.memNo()))
                .map(m -> toBizEntity(m, now, false))
                .toList();

            if (!bizInsert.isEmpty()) {
                memberRepository.bulkInsertBusinesses(bizInsert);
            }
            if (!bizUpdate.isEmpty()) {
                memberRepository.bulkUpdateBusinesses(bizUpdate, MemberUploadType.LIST);
            }

            // 중복 사업자번호 검사
            checkDuplicateBusinessNo(bizMembers);
        }

        // 거래처 자동 생성 (신규 회원만, 이미 있으면 스킵)
        autoCreatePartners(toInsert, crawledMembers, now);

        return String.format("신규 %d건, 업데이트 %d건", toInsert.size(), toUpdate.size());
    }

    private void checkDuplicateBusinessNo(List<CrawledMember> bizMembers) {
        List<Long> memNos = new ArrayList<>();
        List<String> duplicateMemNos = new ArrayList<>();

        for (CrawledMember m : bizMembers) {
            if (m.businessNo() == null || m.businessNo().isBlank()) {
                continue;
            }
            List<Long> others = memberRepository.findOtherMemNosByBusinessNo(m.businessNo(), m.memNo());
            String duplicateValue = others.isEmpty() ? null
                : others.stream().map(String::valueOf).collect(Collectors.joining(";"));
            memNos.add(m.memNo());
            duplicateMemNos.add(duplicateValue);
        }

        if (!memNos.isEmpty()) {
            memberRepository.bulkUpdateDuplicateMemNos(memNos, duplicateMemNos);
        }
    }

    private void autoCreatePartners(List<GodoMemberEntity> newMembers,
                                    List<CrawledMember> allCrawled, String now) {
        if (newMembers.isEmpty()) return;

        Set<Long> newMemNos = newMembers.stream()
            .map(GodoMemberEntity::getMemNo).collect(Collectors.toSet());
        Set<Long> existingPartnerNos = new HashSet<>(
            memberRepository.findExistingPartnerNos(new ArrayList<>(newMemNos)));

        Map<Long, CrawledMember> crawledByMemNo = allCrawled.stream()
            .collect(Collectors.toMap(CrawledMember::memNo, m -> m));

        int baseCode = memberRepository.findMaxPartnerCodeNumeric();
        List<MemberPartnerEntity> toInsert = new ArrayList<>();
        for (GodoMemberEntity member : newMembers) {
            if (existingPartnerNos.contains(member.getMemNo())) continue;

            String partnerCode = String.valueOf(baseCode + 1 + toInsert.size());

            CrawledMember crawled = crawledByMemNo.get(member.getMemNo());
            String company = crawled != null ? crawled.company() : null;
            String partnerName = "(에쓰푸드몰)" + (company != null ? company : "");

            MemberPartnerEntity partner = new MemberPartnerEntity();
            partner.setMemNo(member.getMemNo());
            partner.setPartnerCode(partnerCode);
            partner.setPartnerName(partnerName);
            partner.setTaxInvoiceFl("n");
            partner.setMemberStatus(MemberPartnerStatus.UNREGISTERED.name());
            partner.setCreatedAt(now);
            partner.setUpdatedAt(now);
            toInsert.add(partner);
        }

        if (!toInsert.isEmpty()) {
            memberRepository.bulkInsertPartners(toInsert);
        }
    }

    private GodoMemberEntity toEntity(CrawledMember m, String now, boolean isNew) {
        GodoMemberEntity entity = new GodoMemberEntity();
        entity.setMemNo(m.memNo());
        entity.setMemId(m.memId());

        if (m.groupSno() != null) {
            entity.setGroupSno(m.groupSno());
        }
        if (m.memberFl() != null) {
            entity.setMemberFl(m.memberFl());
        }
        if (m.appFl() != null) {
            entity.setAppFl(m.appFl());
        }
        entity.setAppDt(m.appDt());

        // PII 필드: 평문 저장 (암호화는 MemberRepositoryImpl.enc() 에서 일괄 처리)
        entity.setMemName(m.memName());
        entity.setMemNameHash(hashUtil.hash(m.memName()));
        entity.setEmail(m.email());
        entity.setEmailHash(hashUtil.hash(m.email() != null ? m.email().toLowerCase() : null));
        entity.setCellPhone(m.cellPhone());
        String cellPhoneDigits = m.cellPhone() != null
            ? m.cellPhone().replaceAll("[^0-9]", "")
            : null;
        entity.setCellPhoneHash(hashUtil.hash(cellPhoneDigits));
        entity.setPhone(m.phone());
        entity.setAddress(m.address());
        entity.setBirthDt(m.birthDt());

        // 일반 필드
        entity.setNickName(m.nickName());
        entity.setSexFl(m.sexFl());
        entity.setMarriFl(m.marriFl());
        entity.setMarriDate(m.marriDate());
        entity.setJob(m.job());
        entity.setInterest(m.interest());
        if (m.maillingFl() != null) {
            entity.setMaillingFl(m.maillingFl());
        }
        if (m.smsFl() != null) {
            entity.setSmsFl(m.smsFl());
        }
        entity.setFax(m.fax());
        entity.setEntryDt(m.entryDt());
        if (m.entryPath() != null) {
            entity.setEntryPath(m.entryPath());
        }
        entity.setLastLoginDt(m.lastLoginDt());
        if (m.loginCount() != null) {
            entity.setLoginCount(m.loginCount());
        }
        if (m.saleAmount() != null) {
            entity.setSaleAmount(m.saleAmount());
        }
        if (m.mileage() != null) {
            entity.setMileage(m.mileage());
        }
        if (m.deposit() != null) {
            entity.setDeposit(m.deposit());
        }
        entity.setRecommId(m.recommId());
        entity.setEx1(m.ex1());
        entity.setEx2(m.ex2());
        entity.setEx3(m.ex3());
        if (m.expirationFl() != null) {
            entity.setExpirationFl(m.expirationFl());
        }
        entity.setMemo(m.memo());
        entity.setDeletedFl("n");
        entity.setSyncedAt(now);
        entity.setUpdatedAt(now);
        if (isNew) {
            entity.setCreatedAt(now);
        }
        return entity;
    }

    private GodoMemberBusinessEntity toBizEntity(CrawledMember m, String now, boolean isNew) {
        GodoMemberBusinessEntity entity = new GodoMemberBusinessEntity();
        entity.setMemNo(m.memNo());
        entity.setCompany(m.company());
        entity.setBusinessNo(m.businessNo());
        entity.setCeoName(m.ceoName());
        entity.setServiceName(m.service());
        entity.setServiceCode(commonCodeMappingService.findServiceCode(m.service()).orElse(null));
        entity.setItemName(m.item());
        entity.setItemCode(commonCodeMappingService.findItemCode(m.item()).orElse(null));
        entity.setComAddress(m.comAddress());
        entity.setUpdatedAt(now);
        if (isNew) {
            entity.setCreatedAt(now);
        }
        return entity;
    }
}
