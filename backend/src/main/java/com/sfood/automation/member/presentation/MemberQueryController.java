package com.sfood.automation.member.presentation;

import com.sfood.automation.member.application.dto.MemberDetailResponse;
import com.sfood.automation.member.application.dto.MemberListItemResponse;
import com.sfood.automation.member.application.dto.MemberPageResponse;
import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.application.dto.MemberSummaryResponse;
import com.sfood.automation.member.application.usecase.GetMemberDetailUseCase;
import com.sfood.automation.member.application.usecase.GetMemberListUseCase;
import com.sfood.automation.member.application.usecase.GetMemberSummaryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberQueryController {

    private final GetMemberListUseCase getMemberListUseCase;
    private final GetMemberSummaryUseCase getMemberSummaryUseCase;
    private final GetMemberDetailUseCase getMemberDetailUseCase;

    public MemberQueryController(
        GetMemberListUseCase getMemberListUseCase,
        GetMemberSummaryUseCase getMemberSummaryUseCase,
        GetMemberDetailUseCase getMemberDetailUseCase
    ) {
        this.getMemberListUseCase = getMemberListUseCase;
        this.getMemberSummaryUseCase = getMemberSummaryUseCase;
        this.getMemberDetailUseCase = getMemberDetailUseCase;
    }

    @GetMapping
    public ResponseEntity<MemberPageResponse<MemberListItemResponse>> getMemberList(
        @ModelAttribute MemberSearchCondition condition
    ) {
        return ResponseEntity.ok(getMemberListUseCase.execute(condition));
    }

    @GetMapping("/summary")
    public ResponseEntity<MemberSummaryResponse> getMemberSummary() {
        return ResponseEntity.ok(getMemberSummaryUseCase.execute());
    }

    @GetMapping("/{memNo}")
    public ResponseEntity<MemberDetailResponse> getMemberDetail(@PathVariable Long memNo) {
        return ResponseEntity.ok(getMemberDetailUseCase.execute(memNo));
    }
}
