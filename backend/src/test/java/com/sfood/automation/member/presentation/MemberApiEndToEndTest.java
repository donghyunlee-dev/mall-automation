package com.sfood.automation.member.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.member.application.upload.MemberRowFixture;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberBusinessJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.GodoMemberJpaRepository;
import com.sfood.automation.member.infrastructure.persistence.MemberPartnerJpaRepository;
import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** 업로드 → 목록(마스킹) → 요약 → 상세(평문) HTTP 전 구간 검증 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberApiEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GodoMemberJpaRepository godoMemberJpaRepository;

    @Autowired
    private GodoMemberBusinessJpaRepository godoMemberBusinessJpaRepository;

    @Autowired
    private MemberPartnerJpaRepository memberPartnerJpaRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberPartnerJpaRepository.deleteAll();
        godoMemberBusinessJpaRepository.deleteAll();
        godoMemberJpaRepository.deleteAll();
    }

    @Test
    void should_upload_then_query_list_summary_and_detail_over_http() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        // 단일 API 업로드
        mockMvc.perform(post("/api/members/uploads")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "LIST",
                    "fileName", "list.xlsx",
                    "applyDeletion", false,
                    "rows", List.of(MemberRowFixture.listRow())))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalInserted").value(1))
            .andExpect(jsonPath("$.totalUpdated").value(0))
            .andExpect(jsonPath("$.totalFailed").value(0));

        // 목록 — 마스킹 확인 및 사업자 정보 포함 확인
        mockMvc.perform(get("/api/members").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].memName").value("홍*동"))
            .andExpect(jsonPath("$.content[0].email").value("ho***@example.com"))
            .andExpect(jsonPath("$.content[0].company").value("(주)테스트"))
            .andExpect(jsonPath("$.content[0].businessNo").value("123-45-67890"))
            .andExpect(jsonPath("$.content[0].ceoName").value("김대표"))
            .andExpect(jsonPath("$.content[0].serviceName").value("도매"))
            .andExpect(jsonPath("$.content[0].itemName").value("식품"));

        // 요약
        mockMvc.perform(get("/api/members/summary").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.b2bCount").value(1))
            .andExpect(jsonPath("$.lastSyncedAt").isNotEmpty());

        // 상세 — 복호화 평문 확인
        mockMvc.perform(get("/api/members/900101").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memName").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
            .andExpect(jsonPath("$.cellPhone").value("010-1234-5678"))
            .andExpect(jsonPath("$.address").value("06000 서울시 강남구 테헤란로 1"))
            .andExpect(jsonPath("$.business.businessNo").value("123-45-67890"))
            .andExpect(jsonPath("$.business.ceoName").value("김대표"));

        // 없는 회원 404
        mockMvc.perform(get("/api/members/99999999").session(session))
            .andExpect(status().isNotFound());
    }
}
