package com.sfood.automation.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

class SessionAuthInterceptorTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new SessionAuthTestController())
            .addInterceptors(new SessionAuthInterceptor(new ObjectMapper().findAndRegisterModules()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void should_allow_whitelist_request_without_session() throws Exception {
        mockMvc.perform(post("/api/auth/email-verifications"))
            .andExpect(status().isOk());
    }

    @Test
    void should_reject_protected_request_without_session() throws Exception {
        mockMvc.perform(get("/api/goods"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void should_allow_protected_request_with_valid_session() throws Exception {
        mockMvc.perform(get("/api/goods").sessionAttr(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com"))
            .andExpect(status().isOk());
    }

    @Test
    void should_allow_health_check_without_session() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void should_reject_protected_request_with_invalidated_session() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        session.invalidate();

        mockMvc.perform(get("/api/goods").session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @RestController
    public static class SessionAuthTestController {

        @PostMapping("/api/auth/email-verifications")
        public ResponseEntity<Void> requestVerification() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/auth/session")
        public ResponseEntity<Void> sessionStatus() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/goods")
        public ResponseEntity<Void> goods() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/actuator/health")
        public ResponseEntity<Void> health() {
            return ResponseEntity.ok().build();
        }
    }
}
