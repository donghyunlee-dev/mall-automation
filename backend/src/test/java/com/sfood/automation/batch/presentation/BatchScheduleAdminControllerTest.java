package com.sfood.automation.batch.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.batch.application.dto.BatchRunResponse;
import com.sfood.automation.batch.application.dto.BatchScheduleResponse;
import com.sfood.automation.batch.application.usecase.GetBatchScheduleUseCase;
import com.sfood.automation.batch.application.usecase.RunBatchNowUseCase;
import com.sfood.automation.batch.application.usecase.UpdateBatchScheduleUseCase;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BatchScheduleAdminController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class BatchScheduleAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetBatchScheduleUseCase getBatchScheduleUseCase;

    @MockBean
    private UpdateBatchScheduleUseCase updateBatchScheduleUseCase;

    @MockBean
    private RunBatchNowUseCase runBatchNowUseCase;

    @Test
    void should_return_schedule() throws Exception {
        when(getBatchScheduleUseCase.execute("product-sync")).thenReturn(new BatchScheduleResponse(
            "product-sync",
            true,
            600,
            false,
            "2026-04-01T00:00:00Z",
            "2026-04-01T00:00:10Z",
            "SUCCESS",
            "품절 1건, 정상 전환 0건"
        ));

        mockMvc.perform(get("/api/admin/batch-schedules/product-sync").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobName").value("product-sync"))
            .andExpect(jsonPath("$.fixedDelaySeconds").value(600));
    }

    @Test
    void should_update_schedule() throws Exception {
        mockMvc.perform(put("/api/admin/batch-schedules/product-sync").session(authenticatedSession())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "enabled", true,
                    "fixedDelaySeconds", 300,
                    "modifiedBy", "user@s-food.com"
                ))))
            .andExpect(status().isOk());
    }

    @Test
    void should_run_batch_now() throws Exception {
        when(runBatchNowUseCase.execute(eq("product-sync"))).thenReturn(new BatchRunResponse(true, "STARTED"));

        mockMvc.perform(post("/api/admin/batch-schedules/product-sync/run").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.triggered").value(true))
            .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    void should_return_bad_request_when_update_request_is_invalid() throws Exception {
        mockMvc.perform(put("/api/admin/batch-schedules/product-sync").session(authenticatedSession())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.Map.of(
                    "enabled", true,
                    "modifiedBy", ""
                ))))
            .andExpect(status().isBadRequest());
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return session;
    }
}
