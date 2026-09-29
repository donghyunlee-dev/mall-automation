package com.sfood.automation.member.presentation;

import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.member.application.dto.MemberBusinessLogListResponse;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateRequest;
import com.sfood.automation.member.application.dto.MemberBusinessUpdateResponse;
import com.sfood.automation.member.application.dto.MemberPartnerLogListResponse;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateRequest;
import com.sfood.automation.member.application.dto.MemberPartnerUpdateResponse;
import com.sfood.automation.member.application.usecase.DeleteMemberUseCase;
import com.sfood.automation.member.application.usecase.GetMemberBusinessLogsUseCase;
import com.sfood.automation.member.application.usecase.GetMemberPartnerLogsUseCase;
import com.sfood.automation.member.application.usecase.UpdateMemberBusinessUseCase;
import com.sfood.automation.member.application.usecase.UpdateMemberPartnerUseCase;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberManageController {

    private final UpdateMemberPartnerUseCase updateMemberPartnerUseCase;
    private final UpdateMemberBusinessUseCase updateMemberBusinessUseCase;
    private final GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase;
    private final GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase;
    private final DeleteMemberUseCase deleteMemberUseCase;

    public MemberManageController(
        UpdateMemberPartnerUseCase updateMemberPartnerUseCase,
        UpdateMemberBusinessUseCase updateMemberBusinessUseCase,
        GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase,
        GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase,
        DeleteMemberUseCase deleteMemberUseCase
    ) {
        this.updateMemberPartnerUseCase = updateMemberPartnerUseCase;
        this.updateMemberBusinessUseCase = updateMemberBusinessUseCase;
        this.getMemberBusinessLogsUseCase = getMemberBusinessLogsUseCase;
        this.getMemberPartnerLogsUseCase = getMemberPartnerLogsUseCase;
        this.deleteMemberUseCase = deleteMemberUseCase;
    }

    @PutMapping("/{memNo}/partner")
    public ResponseEntity<MemberPartnerUpdateResponse> updatePartner(
        @PathVariable Long memNo,
        @RequestBody MemberPartnerUpdateRequest request,
        HttpSession session
    ) {
        Object emailAttr = session == null ? null : session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
        String loggedBy = emailAttr != null ? emailAttr.toString() : null;
        return ResponseEntity.ok(updateMemberPartnerUseCase.execute(memNo, request, loggedBy));
    }

    @PutMapping("/{memNo}/business")
    public ResponseEntity<MemberBusinessUpdateResponse> updateBusiness(
        @PathVariable Long memNo,
        @RequestBody MemberBusinessUpdateRequest request,
        HttpSession session
    ) {
        Object emailAttr = session == null ? null : session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
        String loggedBy = emailAttr != null ? emailAttr.toString() : null;
        return ResponseEntity.ok(updateMemberBusinessUseCase.execute(memNo, request, loggedBy));
    }

    @GetMapping("/{memNo}/business/logs")
    public ResponseEntity<MemberBusinessLogListResponse> getBusinessLogs(@PathVariable Long memNo) {
        return ResponseEntity.ok(getMemberBusinessLogsUseCase.execute(memNo));
    }

    @GetMapping("/{memNo}/partner/logs")
    public ResponseEntity<MemberPartnerLogListResponse> getPartnerLogs(@PathVariable Long memNo) {
        return ResponseEntity.ok(getMemberPartnerLogsUseCase.execute(memNo));
    }

    @DeleteMapping("/{memNo}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memNo) {
        deleteMemberUseCase.execute(memNo);
        return ResponseEntity.noContent().build();
    }
}
