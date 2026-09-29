package com.sfood.automation.member.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.exception.DuplicatePartnerCodeException;
import com.sfood.automation.common.exception.MemberNotFoundException;
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

@WebMvcTest(controllers = MemberManageController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class MemberManageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UpdateMemberPartnerUseCase updateMemberPartnerUseCase;

    @MockBean
    private UpdateMemberBusinessUseCase updateMemberBusinessUseCase;

    @MockBean
    private GetMemberBusinessLogsUseCase getMemberBusinessLogsUseCase;

    @MockBean
    private GetMemberPartnerLogsUseCase getMemberPartnerLogsUseCase;

    @MockBean
    private DeleteMemberUseCase deleteMemberUseCase;

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return session;
    }

    @Test
    void should_return_200_with_updated_partner() throws Exception {
        when(updateMemberPartnerUseCase.execute(eq(900101L), any(MemberPartnerUpdateRequest.class), any()))
            .thenReturn(new MemberPartnerUpdateResponse(
                "990954", "(주)테스트", "n", "NORMAL", null, "2026-06-15T00:00:00Z"));

        mockMvc.perform(put("/api/members/900101/partner")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "partnerCode", "990954",
                    "partnerName", "(주)테스트",
                    "taxInvoiceFl", "n",
                    "memberStatus", "NORMAL"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberStatus").value("NORMAL"))
            .andExpect(jsonPath("$.partnerCode").value("990954"))
            .andExpect(jsonPath("$.partnerName").value("(주)테스트"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void should_return_404_for_unknown_member() throws Exception {
        when(updateMemberPartnerUseCase.execute(eq(99999L), any(MemberPartnerUpdateRequest.class), any()))
            .thenThrow(new MemberNotFoundException());

        mockMvc.perform(put("/api/members/99999/partner")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("memberStatus", "UNREGISTERED"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void should_return_200_when_partner_record_is_created() throws Exception {
        when(updateMemberPartnerUseCase.execute(eq(900102L), any(MemberPartnerUpdateRequest.class), any()))
            .thenReturn(new MemberPartnerUpdateResponse(
                null, null, "n", "UNREGISTERED", null, "2026-06-15T00:00:00Z"));

        mockMvc.perform(put("/api/members/900102/partner")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("memberStatus", "UNREGISTERED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberStatus").value("UNREGISTERED"));
    }

    @Test
    void should_return_409_when_partner_code_is_duplicate() throws Exception {
        when(updateMemberPartnerUseCase.execute(eq(900101L), any(MemberPartnerUpdateRequest.class), any()))
            .thenThrow(new DuplicatePartnerCodeException());

        mockMvc.perform(put("/api/members/900101/partner")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "partnerCode", "12345",
                    "memberStatus", "NORMAL"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_PARTNER_CODE"));
    }

    @Test
    void should_return_401_without_authenticated_session() throws Exception {
        mockMvc.perform(put("/api/members/900101/partner")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("memberStatus", "UNREGISTERED"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_204_on_successful_delete() throws Exception {
        doNothing().when(deleteMemberUseCase).execute(900101L);

        mockMvc.perform(delete("/api/members/900101").session(authenticatedSession()))
            .andExpect(status().isNoContent());
    }

    @Test
    void should_return_404_when_member_not_found_on_delete() throws Exception {
        doThrow(new MemberNotFoundException()).when(deleteMemberUseCase).execute(99999L);

        mockMvc.perform(delete("/api/members/99999").session(authenticatedSession()))
            .andExpect(status().isNotFound());
    }

    @Test
    void should_return_401_on_delete_without_session() throws Exception {
        mockMvc.perform(delete("/api/members/900101"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_200_on_business_update() throws Exception {
        when(updateMemberBusinessUseCase.execute(eq(900101L),
            any(MemberBusinessUpdateRequest.class), any()))
            .thenReturn(new MemberBusinessUpdateResponse(
                "수정상호", "111-22-33333", null, null, null, null, null, null, null,
                "2026-06-15T00:00:00Z"));

        mockMvc.perform(put("/api/members/900101/business")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "company", "수정상호", "businessNo", "111-22-33333"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.company").value("수정상호"))
            .andExpect(jsonPath("$.businessNo").value("111-22-33333"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void should_return_200_on_get_business_logs() throws Exception {
        when(getMemberBusinessLogsUseCase.execute(900101L))
            .thenReturn(new MemberBusinessLogListResponse(List.of()));

        mockMvc.perform(get("/api/members/900101/business/logs").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logs").isArray());
    }

    @Test
    void should_return_200_on_get_partner_logs() throws Exception {
        when(getMemberPartnerLogsUseCase.execute(900101L))
            .thenReturn(new MemberPartnerLogListResponse(List.of()));

        mockMvc.perform(get("/api/members/900101/partner/logs").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logs").isArray());
    }
}
