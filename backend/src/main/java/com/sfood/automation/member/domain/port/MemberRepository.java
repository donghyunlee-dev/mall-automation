package com.sfood.automation.member.domain.port;

import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.domain.enums.MemberUploadType;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessLogEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerEntity;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerLogEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    Optional<GodoMemberEntity> findByMemNo(Long memNo);

    MemberListPage findPage(MemberSearchCondition condition);

    MemberSummaryCounts findSummary();

    void save(GodoMemberEntity member);

    void saveAll(List<GodoMemberEntity> members);

    List<Long> findAllActiveMemNos();

    int softDeleteByMemNos(Collection<Long> memNos, String updatedAt);

    Optional<GodoMemberBusinessEntity> findBusinessByMemNo(Long memNo);

    void saveBusiness(GodoMemberBusinessEntity business);

    Optional<MemberPartnerEntity> findPartnerByMemNo(Long memNo);

    boolean existsActivePartnerCode(String partnerCode, Long excludeMemNo);

    void savePartner(MemberPartnerEntity partner);

    // bulk upsert (단일 업로드 API용)
    List<Long> findExistingMemNos(List<Long> memNos);

    List<Long> findExistingBusinessNos(List<Long> memNos);

    List<Long> findExistingPartnerNos(List<Long> memNos);

    void bulkInsertMembers(List<GodoMemberEntity> members);

    void bulkUpdateMembers(List<GodoMemberEntity> members);

    void bulkInsertBusinesses(List<GodoMemberBusinessEntity> businesses);

    void bulkUpdateBusinesses(List<GodoMemberBusinessEntity> businesses, MemberUploadType type);

    void bulkInsertPartners(List<MemberPartnerEntity> partners);

    void bulkUpdatePartners(List<MemberPartnerEntity> partners);

    void deleteMember(Long memNo);

    int findMaxPartnerCodeNumeric();

    void saveBusinessLog(GodoMemberBusinessLogEntity log);

    void savePartnerLog(MemberPartnerLogEntity log);

    List<GodoMemberBusinessLogEntity> findBusinessLogsByMemNo(Long memNo);

    List<MemberPartnerLogEntity> findPartnerLogsByMemNo(Long memNo);

    List<Long> findOtherMemNosByBusinessNo(String businessNo, Long excludeMemNo);

    void bulkUpdateDuplicateMemNos(List<Long> memNos, List<String> duplicateMemNos);
}
