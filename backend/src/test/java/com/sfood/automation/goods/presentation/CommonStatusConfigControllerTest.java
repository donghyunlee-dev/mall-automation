package com.sfood.automation.goods.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.goods.application.dto.CommonStatusConfigResponse;
import com.sfood.automation.goods.application.dto.UpdateCommonStatusConfigRequest;
import com.sfood.automation.goods.application.usecase.GetCommonStatusConfigUseCase;
import com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCase;
import com.sfood.automation.goods.domain.enums.GoodsScope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CommonStatusConfigController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class CommonStatusConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetCommonStatusConfigUseCase getUseCase;

    @MockBean
    private UpdateCommonStatusConfigUseCase updateUseCase;

    @Test
    void should_return_all_configs_as_list() throws Exception {
        when(getUseCase.execute())
            .thenReturn(List.of(
                new CommonStatusConfigResponse("GENERAL", 20, 21, 50, 150),
                new CommonStatusConfigResponse("EXCLUSIVE", 1, 2, 50, 150)
            ));

        MockHttpSession session = authenticatedSession();
        mockMvc.perform(get("/api/config/status").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].scope").value("GENERAL"))
            .andExpect(jsonPath("$[1].scope").value("EXCLUSIVE"));
    }

    @Test
    void should_update_config() throws Exception {
        when(updateUseCase.execute(eq(GoodsScope.GENERAL), any(), eq("user@s-food.com")))
            .thenReturn(new CommonStatusConfigResponse("GENERAL", 10, 11, 40, 150));

        MockHttpSession session = authenticatedSession();
        mockMvc.perform(put("/api/config/status/GENERAL").session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdateCommonStatusConfigRequest(10, 11, 40, 150))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scope").value("GENERAL"))
            .andExpect(jsonPath("$.outOfStockQty").value(10));
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        return session;
    }
}
