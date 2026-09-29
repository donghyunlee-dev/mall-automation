package com.sfood.automation.auth.presentation;

import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.dto.MsLoginVerifyRequest;
import com.sfood.automation.auth.application.usecase.ConfirmMsLoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/ms-login")
public class MsLoginController {

    private final ConfirmMsLoginUseCase confirmMsLoginUseCase;

    public MsLoginController(ConfirmMsLoginUseCase confirmMsLoginUseCase) {
        this.confirmMsLoginUseCase = confirmMsLoginUseCase;
    }

    @PostMapping("/verify")
    public ResponseEntity<ConfirmVerificationResponse> verify(
        @Valid @RequestBody MsLoginVerifyRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(confirmMsLoginUseCase.execute(request.loginToken(), httpServletRequest));
    }
}
