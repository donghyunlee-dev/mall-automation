package com.sfood.automation.member.domain.port;

import java.util.List;

public record MemberListPage(
    List<MemberListRow> rows,
    long totalElements
) {}
