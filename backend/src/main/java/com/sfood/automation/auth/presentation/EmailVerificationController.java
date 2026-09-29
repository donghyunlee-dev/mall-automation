package com.sfood.automation.auth.presentation;

import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.sfood.automation.auth.application.dto.ConfirmVerificationRequest;
import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.dto.CurrentUserResponse;
import com.sfood.automation.auth.application.dto.EmailVerificationRequest;
import com.sfood.automation.auth.application.dto.EmailVerificationResponse;
import com.sfood.automation.auth.application.dto.UpdateUserNotificationsRequest;
import com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCase;
import com.sfood.automation.auth.application.usecase.GetCurrentUserUseCase;
import com.sfood.automation.auth.application.usecase.RequestEmailVerificationUseCase;
import com.sfood.automation.auth.application.usecase.UpdateUserNotificationsUseCase;
import com.sfood.automation.common.exception.ErrorCode;
import com.sfood.automation.common.exception.ErrorResponse;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class EmailVerificationController {

    private final RequestEmailVerificationUseCase requestUseCase;
    private final ConfirmEmailVerificationUseCase confirmUseCase;
    private final AuthSessionPort authSessionPort;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateUserNotificationsUseCase updateUserNotificationsUseCase;

    public EmailVerificationController(
        RequestEmailVerificationUseCase requestUseCase,
        ConfirmEmailVerificationUseCase confirmUseCase,
        AuthSessionPort authSessionPort,
        GetCurrentUserUseCase getCurrentUserUseCase,
        UpdateUserNotificationsUseCase updateUserNotificationsUseCase
    ) {
        this.requestUseCase = requestUseCase;
        this.confirmUseCase = confirmUseCase;
        this.authSessionPort = authSessionPort;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateUserNotificationsUseCase = updateUserNotificationsUseCase;
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<EmailVerificationResponse> requestVerification(
        @Valid @RequestBody EmailVerificationRequest request,
        HttpServletRequest httpServletRequest
    ) {
        EmailVerificationResponse response = requestUseCase.execute(
            request.email(),
            httpServletRequest.getRemoteAddr(),
            httpServletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<ConfirmVerificationResponse> confirmVerification(
        @Valid @RequestBody ConfirmVerificationRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(confirmUseCase.execute(
            request.verificationKey(),
            request.email(),
            request.code(),
            httpServletRequest
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authSessionPort.invalidate(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {
        Object email = sessionEmail(request);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.from(ErrorCode.AUTHENTICATION_REQUIRED));
        }
        return ResponseEntity.ok(Map.of("email", email));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(HttpServletRequest request) {
        return ResponseEntity.ok(getCurrentUserUseCase.execute(sessionEmail(request)));
    }

    @org.springframework.web.bind.annotation.PutMapping("/me/notifications")
    public ResponseEntity<CurrentUserResponse> updateUserNotifications(
        @Valid @RequestBody UpdateUserNotificationsRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(updateUserNotificationsUseCase.execute(sessionEmail(httpServletRequest), request));
    }

    private String sessionEmail(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            Object email = session == null ? null : session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
            return email == null ? null : email.toString();
        } catch (IllegalStateException exception) {
            return null;
        }
    }
}
