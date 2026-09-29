package com.sfood.automation.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.auth.application.dto.ConfirmVerificationResponse;
import com.sfood.automation.auth.application.usecase.ConfirmMsLoginUseCase;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.exception.MsLoginVerificationFailedException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MsLoginController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, com.sfood.automation.common.security.SessionAuthInterceptor.class})
class MsLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfirmMsLoginUseCase confirmMsLoginUseCase;

    @Test
    void should_return_bad_request_when_login_token_missing() throws Exception {
        mockMvc.perform(post("/api/auth/ms-login/verify")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_ok_when_verification_succeeds() throws Exception {
        when(confirmMsLoginUseCase.execute(eq("token-abc"), any()))
            .thenReturn(new ConfirmVerificationResponse(true, "user@s-food.com"));

        mockMvc.perform(post("/api/auth/ms-login/verify")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("loginToken", "token-abc"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.email").value("user@s-food.com"));
    }

    @Test
    void should_return_unauthorized_when_verification_fails() throws Exception {
        when(confirmMsLoginUseCase.execute(eq("token-abc"), any()))
            .thenThrow(new MsLoginVerificationFailedException("TOKEN_EXPIRED"));

        mockMvc.perform(post("/api/auth/ms-login/verify")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("loginToken", "token-abc"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("MS_LOGIN_VERIFICATION_FAILED"))
            .andExpect(jsonPath("$.message").value("TOKEN_EXPIRED"));
    }
}
