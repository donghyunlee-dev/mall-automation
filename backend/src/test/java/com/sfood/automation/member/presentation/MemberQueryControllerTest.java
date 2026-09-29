package com.sfood.automation.member.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.exception.MemberNotFoundException;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.member.application.dto.MemberBusinessDetailResponse;
import com.sfood.automation.member.application.dto.MemberDetailResponse;
import com.sfood.automation.member.application.dto.MemberListItemResponse;
import com.sfood.automation.member.application.dto.MemberPageResponse;
import com.sfood.automation.member.application.dto.MemberSearchCondition;
import com.sfood.automation.member.application.dto.MemberSummaryResponse;
import com.sfood.automation.member.application.usecase.GetMemberDetailUseCase;
import com.sfood.automation.member.application.usecase.GetMemberListUseCase;
import com.sfood.automation.member.application.usecase.GetMemberSummaryUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberQueryController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class MemberQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMemberListUseCase getMemberListUseCase;

    @MockBean
    private GetMemberSummaryUseCase getMemberSummaryUseCase;

    @MockBean
    private GetMemberDetailUseCase getMemberDetailUseCase;

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return session;
    }

    private MemberPageResponse<MemberListItemResponse> samplePage() {
        return new MemberPageResponse<>(
            List.of(new MemberListItemResponse(
                900101L, "user1", "홍*동", 9, "B2B 거래처",
                "ho***@example.com", "010-****-5678", "(주)테스트", "123-45-67890", "홍대표",
                "도매", "식품", "06000 서울시 강남구 테헤란로 1", "990954", "(에쓰푸드몰)테스트",
                "y", "2024-01-01 12:00:00", "y", null)),
            0, 20, 1, 1);
    }

    @Test
    void should_return_paged_member_list() throws Exception {
        when(getMemberListUseCase.execute(any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/members").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].memNo").value(900101))
            .andExpect(jsonPath("$.content[0].memName").value("홍*동"))
            .andExpect(jsonPath("$.content[0].cellPhone").value("010-****-5678"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_duplicate_mem_no_in_member_list() throws Exception {
        MemberPageResponse<MemberListItemResponse> page = new MemberPageResponse<>(
            List.of(new MemberListItemResponse(
                900101L, "user1", "홍*동", 9, "B2B 거래처",
                "ho***@example.com", "010-****-5678", "(주)테스트", "123-45-67890", "홍대표",
                "도매", "식품", "06000 서울시 강남구 테헤란로 1", "990954", "(에쓰푸드몰)테스트",
                "y", "2024-01-01 12:00:00", "y", "900102;900103")),
            0, 20, 1, 1);
        when(getMemberListUseCase.execute(any())).thenReturn(page);

        mockMvc.perform(get("/api/members").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].duplicateMemNo").value("900102;900103"));
    }

    @Test
    void should_bind_search_parameters() throws Exception {
        when(getMemberListUseCase.execute(any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/members")
                .param("searchType", "NAME")
                .param("keyword", "홍길동")
                .param("group", "B2B")
                .param("appFl", "n")
                .param("page", "2")
                .param("size", "50")
                .session(authenticatedSession()))
            .andExpect(status().isOk());

        ArgumentCaptor<MemberSearchCondition> captor = ArgumentCaptor.forClass(MemberSearchCondition.class);
        verify(getMemberListUseCase).execute(captor.capture());
        MemberSearchCondition condition = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(condition.searchType())
            .isEqualTo(MemberSearchCondition.SearchType.NAME);
        org.assertj.core.api.Assertions.assertThat(condition.keyword()).isEqualTo("홍길동");
        org.assertj.core.api.Assertions.assertThat(condition.group())
            .isEqualTo(MemberSearchCondition.GroupFilter.B2B);
        org.assertj.core.api.Assertions.assertThat(condition.appFl()).isEqualTo("n");
        org.assertj.core.api.Assertions.assertThat(condition.page()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(condition.size()).isEqualTo(50);
    }

    @Test
    void should_return_summary_with_five_fields() throws Exception {
        when(getMemberSummaryUseCase.execute())
            .thenReturn(new MemberSummaryResponse(23000, 1200, 300, 15, "2026-06-12T00:00:00Z"));

        mockMvc.perform(get("/api/members/summary").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(23000))
            .andExpect(jsonPath("$.businessCount").value(1200))
            .andExpect(jsonPath("$.b2bCount").value(300))
            .andExpect(jsonPath("$.pendingApprovalCount").value(15))
            .andExpect(jsonPath("$.lastSyncedAt").value("2026-06-12T00:00:00Z"));
    }

    @Test
    void should_return_404_for_unknown_member() throws Exception {
        when(getMemberDetailUseCase.execute(99999L)).thenThrow(new MemberNotFoundException());

        mockMvc.perform(get("/api/members/99999").session(authenticatedSession()))
            .andExpect(status().isNotFound());
    }

    @Test
    void should_return_plaintext_detail_for_existing_member() throws Exception {
        when(getMemberDetailUseCase.execute(eq(900101L))).thenReturn(new MemberDetailResponse(
            900101L, "user1", "홍길동", 9, "B2B 거래처", "y", null,
            "hong@example.com", "010-1234-5678", null, "서울시 강남구", null, null,
            "pc", "y", "y", null, "m", "1990-01-01", null, null, null, null,
            5, 12500, 1500, 0, null,
            null, null, null, null, null, null, null, null, null));

        mockMvc.perform(get("/api/members/900101").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memName").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
            .andExpect(jsonPath("$.cellPhone").value("010-1234-5678"));
    }

    @Test
    void should_return_duplicate_mem_no_in_business_detail() throws Exception {
        MemberBusinessDetailResponse business = new MemberBusinessDetailResponse(
            "(주)테스트", "123-45-67890", "홍대표", "도매", "01", "식품", "01",
            "서울시 강남구", "license.pdf", "900102;900103");
        when(getMemberDetailUseCase.execute(eq(900101L))).thenReturn(new MemberDetailResponse(
            900101L, "user1", "홍길동", 9, "B2B 거래처", "y", null,
            "hong@example.com", "010-1234-5678", null, "서울시 강남구", null, null,
            "pc", "y", "y", null, "m", "1990-01-01", null, null, null, null,
            5, 12500, 1500, 0, null,
            null, null, null, null, null, null, null, business, null));

        mockMvc.perform(get("/api/members/900101").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.business.duplicateMemNo").value("900102;900103"));
    }

    @Test
    void should_return_401_without_authenticated_session() throws Exception {
        mockMvc.perform(get("/api/members"))
            .andExpect(status().isUnauthorized());
    }
}
