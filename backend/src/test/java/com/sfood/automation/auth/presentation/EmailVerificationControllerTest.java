package com.sfood.automation.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfood.automation.auth.application.port.AuthSessionPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.dto.CurrentUserResponse;
import com.sfood.automation.auth.application.dto.EmailVerificationResponse;
import com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCase;
import com.sfood.automation.auth.application.usecase.GetCurrentUserUseCase;
import com.sfood.automation.auth.application.usecase.RequestEmailVerificationUseCase;
import com.sfood.automation.auth.application.usecase.UpdateUserNotificationsUseCase;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.exception.OtpMailDeliveryException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmailVerificationController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, com.sfood.automation.common.security.SessionAuthInterceptor.class})
class EmailVerificationControllerTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestEmailVerificationUseCase requestUseCase;

    @MockBean
    private ConfirmEmailVerificationUseCase confirmUseCase;

    @MockBean
    private AuthSessionPort authSessionPort;

    @MockBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @MockBean
    private UpdateUserNotificationsUseCase updateUserNotificationsUseCase;

    @Test
    void should_return_bad_request_when_email_missing() throws Exception {
        mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_created_for_request_verification() throws Exception {
        when(requestUseCase.execute(eq("user@s-food.com"), any(), any()))
            .thenReturn(new EmailVerificationResponse("key", 300, "Verification code sent"));

        mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@s-food.com"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.verificationKey").value("key"))
            .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void should_return_ok_for_confirm_verification() throws Exception {
        when(confirmUseCase.execute(eq("key"), eq("user@s-food.com"), eq("123456"), any()))
            .thenReturn(new ConfirmVerificationResponse(true, "user@s-food.com"));

        mockMvc.perform(post("/api/auth/email-verifications/confirm")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "verificationKey", "key",
                    "email", "user@s-food.com",
                    "code", "123456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void should_return_bad_request_when_code_has_seven_digits() throws Exception {
        mockMvc.perform(post("/api/auth/email-verifications/confirm")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "verificationKey", "key",
                    "email", "user@s-food.com",
                    "code", "1234567"
                ))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_unauthorized_when_session_missing() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_return_unauthorized_when_session_is_invalidated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(com.sfood.automation.common.security.SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        session.invalidate();

        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_return_bad_gateway_when_otp_mail_send_fails() throws Exception {
        when(requestUseCase.execute(eq("user@s-food.com"), any(), any()))
            .thenThrow(new OtpMailDeliveryException());

        mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@s-food.com"))))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("OTP_MAIL_DELIVERY_FAILED"))
            .andExpect(jsonPath("$.message").value("OTP mail delivery failed"));
    }

    @Test
    void should_return_session_email_when_session_exists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(com.sfood.automation.common.security.SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@s-food.com"));
    }

    @Test
    void should_return_current_user_profile_when_session_exists() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(com.sfood.automation.common.security.SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(getCurrentUserUseCase.execute("user@s-food.com"))
            .thenReturn(new CurrentUserResponse(
                "user@s-food.com",
                "Y",
                "N",
                "2026-04-06T08:00:00Z",
                "2026-04-01T00:00:00Z",
                "2026-04-06T08:00:00Z"
            ));

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@s-food.com"))
            .andExpect(jsonPath("$.emailNotificationYn").value("Y"))
            .andExpect(jsonPath("$.teamsNotificationYn").value("N"));
    }

    @Test
    void should_update_current_user_notifications() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(com.sfood.automation.common.security.SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(updateUserNotificationsUseCase.execute(eq("user@s-food.com"), any()))
            .thenReturn(new CurrentUserResponse(
                "user@s-food.com",
                "Y",
                "Y",
                "2026-04-06T08:00:00Z",
                "2026-04-01T00:00:00Z",
                "2026-04-06T09:00:00Z"
            ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/me/notifications")
                .session(session)
                .contentType("application/json")
                .content("""
                    {"emailNotificationYn":"Y","teamsNotificationYn":"Y"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailNotificationYn").value("Y"))
            .andExpect(jsonPath("$.teamsNotificationYn").value("Y"));
    }

    @Test
    void should_logout_authenticated_session() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(com.sfood.automation.common.security.SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isNoContent());

        verify(authSessionPort).invalidate(any());
    }

    @Test
    void should_reject_logout_when_session_missing() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_allow_cors_preflight_from_frontend_origin() throws Exception {
        mockMvc.perform(options("/api/auth/session")
                .header("Origin", FRONTEND_ORIGIN)
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("GET")));
    }
}
