package com.sfood.automation.member.presentation;

import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse;
import com.sfood.automation.member.application.dto.MemberUploadRequest;
import com.sfood.automation.member.application.usecase.UploadMembersUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/uploads")
public class MemberUploadController {

    private final UploadMembersUseCase uploadMembersUseCase;

    public MemberUploadController(UploadMembersUseCase uploadMembersUseCase) {
        this.uploadMembersUseCase = uploadMembersUseCase;
    }

    @PostMapping
    public ResponseEntity<MemberUploadCompleteResponse> upload(@RequestBody MemberUploadRequest request) {
        return ResponseEntity.ok(uploadMembersUseCase.execute(request));
    }
}
