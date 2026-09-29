package com.sfood.automation.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.auth.application.port.OtpMailPort;
import com.sfood.automation.auth.application.port.OtpCodeGenerator;
import com.sfood.automation.auth.infrastructure.persistence.EmailVerificationJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OtpAuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailVerificationJpaRepository jpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
        registry.add("app.otp.server-secret", () -> "test-secret");
    }

    @AfterEach
    void tearDown() {
        jpaRepository.deleteAll();
    }

    @Test
    void should_complete_otp_flow_and_access_protected_api() throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@s-food.com"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.verificationKey").exists())
            .andReturn();

        String verificationKey = objectMapper.readTree(requestResult.getResponse().getContentAsString())
            .get("verificationKey").asText();

        var stored = jpaRepository.findByVerificationKey(verificationKey).orElseThrow();
        assertThat(stored.getCodeHash()).isNotEqualTo("123456");
        assertThat(stored.getCodeHash()).isNotBlank();

        MvcResult confirmResult = mockMvc.perform(post("/api/auth/email-verifications/confirm")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "verificationKey", verificationKey,
                    "email", "user@s-food.com",
                    "code", "123456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andReturn();

        var session = confirmResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/session").session((org.springframework.mock.web.MockHttpSession) session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@s-food.com"));

        mockMvc.perform(get("/api/goods").session((org.springframework.mock.web.MockHttpSession) session))
            .andExpect(status().isOk());
    }

    @Test
    void should_reject_access_after_logout() throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/api/auth/email-verifications")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of("email", "user@s-food.com"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.verificationKey").exists())
            .andReturn();

        String verificationKey = objectMapper.readTree(requestResult.getResponse().getContentAsString())
            .get("verificationKey").asText();

        MvcResult confirmResult = mockMvc.perform(post("/api/auth/email-verifications/confirm")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "verificationKey", verificationKey,
                    "email", "user@s-food.com",
                    "code", "123456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andReturn();

        var session = (org.springframework.mock.web.MockHttpSession) confirmResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/session").session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"));

        mockMvc.perform(get("/api/goods").session(session))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        OtpCodeGenerator fixedOtpCodeGenerator() {
            return () -> "123456";
        }

        @Bean
        @Primary
        OtpMailPort testOtpMailPort() {
            return (email, code) -> { };
        }
    }
}
