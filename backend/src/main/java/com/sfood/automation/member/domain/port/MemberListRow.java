package com.sfood.automation.member.domain.port;

import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessEntity;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberEntity;

public record MemberListRow(
    GodoMemberEntity member,
    GodoMemberBusinessEntity business,
    String partnerCode,
    String partnerName,
    String memberStatus
) {}
