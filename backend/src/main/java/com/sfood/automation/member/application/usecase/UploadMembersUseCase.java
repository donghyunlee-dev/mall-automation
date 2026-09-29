package com.sfood.automation.member.application.usecase;

import com.sfood.automation.common_code.application.usecase.CommonCodeMappingService;
import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse;
import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse.FailedRow;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.upload.MemberRowNormalizer;
import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.domain.port.MemberRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadMembersUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadMembersUseCase.class);
    private static final double DELETION_GUARD_RATIO = 0.10;

    private final MemberRowNormalizer normalizer;
    private final MemberRepository memberRepository;
    private final CommonCodeMappingService commonCodeMappingService;

    public UploadMembersUseCase(MemberRowNormalizer normalizer,
                                 MemberRepository memberRepository,
                                 CommonCodeMappingService commonCodeMappingService) {
        this.normalizer = normalizer;
        this.memberRepository = memberRepository;
        this.commonCodeMappingService = commonCodeMappingService;
    }

    @Transactional
    public MemberUploadCompleteResponse execute(MemberUploadRequest request) {
        String now = Instant.now().toString();

        int failed = 0;
        List<FailedRow> failedRows = new ArrayList<>();
        Set<Long> receivedMemNos = new HashSet<>();

        // 1. 유효한 행 분리
        List<Long> allMemNos = new ArrayList<>();
        List<List<Object>> validRows = new ArrayList<>();
        List<Integer> originalIndexes = new ArrayList<>();

        for (int i = 0; i < request.rows().size(); i++) {
            Long memNo = normalizer.extractMemNo(request.rows().get(i), request.type());
            if (memNo == null) {
                failed++;
                failedRows.add(new FailedRow(i, "회원번호가 없습니다"));
            } else {
                allMemNos.add(memNo);
                validRows.add(request.rows().get(i));
                originalIndexes.add(i);
            }
        }

        // 2. 기존 레코드 존재 여부 일괄 조회 (테이블당 1쿼리)
        Set<Long> existingMemberNos = new HashSet<>(memberRepository.findExistingMemNos(allMemNos));
        Set<Long> existingBizNos = new HashSet<>(memberRepository.findExistingBusinessNos(allMemNos));
        Set<Long> existingPartnerNos = new HashSet<>(memberRepository.findExistingPartnerNos(allMemNos));

        // 3. 엔티티 목록 구성
        List<GodoMemberEntity> membersToInsert = new ArrayList<>();
        List<GodoMemberEntity> membersToUpdate = new ArrayList<>();
        List<GodoMemberBusinessEntity> bizToInsert = new ArrayList<>();
        List<GodoMemberBusinessEntity> bizToUpdate = new ArrayList<>();
        List<MemberPartnerEntity> partnersToInsert = new ArrayList<>();
        List<MemberPartnerEntity> partnersToUpdate = new ArrayList<>();
        int maxPartnerCode = memberRepository.findMaxPartnerCodeNumeric();

        for (int i = 0; i < validRows.size(); i++) {
            List<Object> row = validRows.get(i);
            Long memNo = allMemNos.get(i);

            try {
                GodoMemberEntity member = new GodoMemberEntity();
                member.setMemNo(memNo);
                member.setCreatedAt(now);
                normalizer.applyMemberFields(member, row, request.type());
                member.setDeletedFl("n");
                member.setSyncedAt(now);
                member.setUpdatedAt(now);
                if (member.getMemId() == null) throw new IllegalArgumentException("아이디가 없습니다");

                if (existingMemberNos.contains(memNo)) membersToUpdate.add(member);
                else membersToInsert.add(member);
                receivedMemNos.add(memNo);

                GodoMemberBusinessEntity biz = new GodoMemberBusinessEntity();
                biz.setMemNo(memNo);
                biz.setCreatedAt(now);
                normalizer.applyBusinessFields(biz, row, request.type());
                biz.setServiceCode(commonCodeMappingService.findServiceCode(biz.getServiceName()).orElse(null));
                biz.setItemCode(commonCodeMappingService.findItemCode(biz.getItemName()).orElse(null));
                biz.setUpdatedAt(now);
                if (biz.getCompany() != null || biz.getBusinessNo() != null) {
                    if (existingBizNos.contains(memNo)) bizToUpdate.add(biz);
                    else bizToInsert.add(biz);
                }

                if (request.type() == MemberUploadType.TEMPLATE) {
                    MemberPartnerEntity partner = new MemberPartnerEntity();
                    partner.setMemNo(memNo);
                    partner.setCreatedAt(now);
                    normalizer.applyPartnerFields(partner, row);
                    partner.setUpdatedAt(now);
                    if (existingPartnerNos.contains(memNo)) partnersToUpdate.add(partner);
                    else partnersToInsert.add(partner);
                } else {
                    // LIST / 기타 타입: 신규 회원만 자동 생성 (기존에 없으면)
                    if (!existingMemberNos.contains(memNo) && !existingPartnerNos.contains(memNo)) {
                        String company = normalizer.extractCompany(row, request.type());
                        MemberPartnerEntity partner = new MemberPartnerEntity();
                        partner.setMemNo(memNo);
                        partner.setPartnerCode(String.valueOf(maxPartnerCode + 1 + partnersToInsert.size()));
                        partner.setPartnerName("(에쓰푸드몰)" + (company != null ? company : ""));
                        partner.setTaxInvoiceFl("n");
                        partner.setMemberStatus(MemberPartnerStatus.UNREGISTERED.name());
                        partner.setCreatedAt(now);
                        partner.setUpdatedAt(now);
                        partnersToInsert.add(partner);
                    }
                }
            } catch (Exception e) {
                log.warn("행 처리 실패 (rowIndex={}): {}", originalIndexes.get(i), e.getMessage());
                failed++;
                failedRows.add(new FailedRow(originalIndexes.get(i), e.getMessage()));
            }
        }

        // 4. 일괄 저장
        memberRepository.bulkInsertMembers(membersToInsert);
        memberRepository.bulkUpdateMembers(membersToUpdate);
        memberRepository.bulkInsertBusinesses(bizToInsert);
        memberRepository.bulkUpdateBusinesses(bizToUpdate, request.type());
        memberRepository.bulkInsertPartners(partnersToInsert);
        memberRepository.bulkUpdatePartners(partnersToUpdate);

        // 5. 소프트 삭제
        String syncedAt = now;
        int totalDeleted = 0;
        boolean deletionSkipped = false;
        String deletionSkippedReason = null;

        if (request.applyDeletion()) {
            List<Long> activeMemNos = memberRepository.findAllActiveMemNos();
            List<Long> notReceived = activeMemNos.stream()
                .filter(memNo -> !receivedMemNos.contains(memNo))
                .toList();
            if (!activeMemNos.isEmpty() && notReceived.size() > activeMemNos.size() * DELETION_GUARD_RATIO) {
                deletionSkipped = true;
                deletionSkippedReason = String.format(
                    "삭제 대상 %d건이 전체 회원 %d건의 10%%를 초과하여 삭제를 적용하지 않았습니다. 파일을 확인해 주세요.",
                    notReceived.size(), activeMemNos.size()
                );
            } else if (!notReceived.isEmpty()) {
                totalDeleted = memberRepository.softDeleteByMemNos(notReceived, syncedAt);
            }
        }

        return new MemberUploadCompleteResponse(
            membersToInsert.size(), membersToUpdate.size(), totalDeleted,
            failed, failedRows, deletionSkipped, deletionSkippedReason, syncedAt
        );
    }
}
