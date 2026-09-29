package com.sfood.automation.common_code.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.common_code.application.dto.CommonCodeListResponse;
import com.sfood.automation.common_code.application.dto.CommonCodeListResponse.CodeItem;
import com.sfood.automation.common_code.application.dto.CommonCodeRequest;
import com.sfood.automation.common_code.application.usecase.CommonCodeUseCase;
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

@WebMvcTest(controllers = CommonCodeController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class CommonCodeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommonCodeUseCase commonCodeUseCase;

    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return s;
    }

    @Test
    void should_return_200_with_code_list() throws Exception {
        when(commonCodeUseCase.findAll(null, null)).thenReturn(new CommonCodeListResponse(List.of(
            new CodeItem(1, "SERVICE", "업태", "001", "도소매", null, "Y", "2026-07-01", "2026-07-01"))));

        mockMvc.perform(get("/api/common-codes").session(session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codes[0].groupCode").value("SERVICE"));
    }

    @Test
    void should_return_active_codes_only_when_useYn_Y() throws Exception {
        when(commonCodeUseCase.findAll("SERVICE", "Y")).thenReturn(new CommonCodeListResponse(List.of(
            new CodeItem(1, "SERVICE", "업태", "046", "식품", null, "Y", "2026-07-01", "2026-07-01"))));

        mockMvc.perform(get("/api/common-codes")
                        .param("groupCode", "SERVICE")
                        .param("useYn", "Y")
                        .session(session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codes[0].code").value("046"));
    }

    @Test
    void should_return_201_on_create() throws Exception {
        doNothing().when(commonCodeUseCase).create(any(CommonCodeRequest.class));

        mockMvc.perform(post("/api/common-codes")
                .session(session())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "groupCode", "SERVICE", "groupName", "업태",
                    "code", "001", "codeName", "도소매", "useYn", "Y"))))
            .andExpect(status().isCreated());
    }

    @Test
    void should_return_200_on_update() throws Exception {
        doNothing().when(commonCodeUseCase).update(eq(1), any(CommonCodeRequest.class));

        mockMvc.perform(put("/api/common-codes/1")
                .session(session())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "groupCode", "SERVICE", "groupName", "업태",
                    "code", "001", "codeName", "소매", "useYn", "Y"))))
            .andExpect(status().isOk());
    }

    @Test
    void should_return_401_without_session() throws Exception {
        mockMvc.perform(get("/api/common-codes"))
            .andExpect(status().isUnauthorized());
    }
}
