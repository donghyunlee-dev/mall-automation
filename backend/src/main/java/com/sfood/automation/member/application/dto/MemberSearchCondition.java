package com.sfood.automation.member.application.dto;

import com.sfood.automation.member.domain.enums.MemberPartnerStatus;
import java.time.LocalDate;
import java.util.Set;

public record MemberSearchCondition(
    SearchType searchType,
    String keyword,
    GroupFilter group,
    String appFl,
    String memberStatus,
    LocalDate entryDtFrom,
    LocalDate entryDtTo,
    String sortKey,
    String sortDir,
    Integer page,
    Integer size
) {
    public enum SearchType { MEM_ID, NAME, EMAIL, CELL_PHONE, PARTNER_CODE, PARTNER_NAME }

    public enum GroupFilter { ALL, GENERAL, BUSINESS, SKILLED, B2B }

    // 암호화 컬럼(이름·이메일·휴대폰)은 DB 정렬 불가 — 허용 목록 외 sortKey는 mem_no desc로 대체
    public static final Set<String> ALLOWED_SORT_KEYS =
        Set.of("mem_no", "mem_id", "group_sno", "app_fl", "entry_dt", "last_login_dt", "sale_amount");

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    public MemberSearchCondition normalized() {
        return new MemberSearchCondition(
            searchType == null ? SearchType.MEM_ID : searchType,
            keyword == null || keyword.isBlank() ? null : keyword.trim(),
            group == null ? GroupFilter.ALL : group,
            "y".equals(appFl) || "n".equals(appFl) ? appFl : null,
            MemberPartnerStatus.isValid(memberStatus) ? memberStatus : null,
            entryDtFrom,
            entryDtTo,
            sortKey != null && ALLOWED_SORT_KEYS.contains(sortKey) ? sortKey : "mem_no",
            "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc",
            page == null || page < 0 ? 0 : page,
            size == null || size <= 0 || size > MAX_SIZE ? DEFAULT_SIZE : size
        );
    }
}
