package com.sfood.automation.member.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.member.application.dto.MemberUploadCompleteResponse;
import com.sfood.automation.member.application.usecase.UploadMembersUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberUploadController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class MemberUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UploadMembersUseCase uploadMembersUseCase;

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return session;
    }

    @Test
    void should_return_200_with_complete_response_for_valid_upload_request() throws Exception {
        when(uploadMembersUseCase.execute(any())).thenReturn(
            new MemberUploadCompleteResponse(100, 0, 0, 0, List.of(), false, null, "2026-06-12T00:00:00Z"));

        mockMvc.perform(post("/api/members/uploads")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "LIST",
                    "fileName", "list.xlsx",
                    "applyDeletion", false,
                    "rows", List.of(List.of(900101, "2024-01-01"))))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalInserted").value(100))
            .andExpect(jsonPath("$.totalUpdated").value(0))
            .andExpect(jsonPath("$.totalDeleted").value(0))
            .andExpect(jsonPath("$.deletionSkipped").value(false));
    }

    @Test
    void should_return_200_with_deletion_skipped_when_guard_exceeded() throws Exception {
        when(uploadMembersUseCase.execute(any())).thenReturn(
            new MemberUploadCompleteResponse(
                1, 0, 0, 0, List.of(), true,
                "삭제 대상 19건이 전체 회원 20건의 10%를 초과하여 삭제를 적용하지 않았습니다. 파일을 확인해 주세요.",
                "2026-06-12T00:00:00Z"));

        mockMvc.perform(post("/api/members/uploads")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "LIST",
                    "fileName", "list.xlsx",
                    "applyDeletion", true,
                    "rows", List.of(List.of(900101))))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deletionSkipped").value(true))
            .andExpect(jsonPath("$.deletionSkippedReason").isNotEmpty());
    }

    @Test
    void should_return_401_without_authenticated_session() throws Exception {
        mockMvc.perform(post("/api/members/uploads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "LIST",
                    "fileName", "list.xlsx",
                    "applyDeletion", false,
                    "rows", List.of()))))
            .andExpect(status().isUnauthorized());
    }
}
