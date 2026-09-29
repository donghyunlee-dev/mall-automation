package com.sfood.automation.member.domain.port;

import com.sfood.automation.member.application.dto.CrawledMember;
import java.time.LocalDate;
import java.util.List;

public interface SfoodAdminMemberPort {
    /** 날짜 기준 신규 가입 미승인 회원 번호 목록 반환. */
    List<Long> fetchMemberNos(LocalDate date);
    /** 회원번호로 상세 팝업에서 전체 필드 수집 */
    CrawledMember fetchMemberDetail(Long memNo);
}
