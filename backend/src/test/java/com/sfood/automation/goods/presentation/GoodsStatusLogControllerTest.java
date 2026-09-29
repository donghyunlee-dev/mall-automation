package com.sfood.automation.goods.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.goods.application.dto.GoodsStatusLogListResponse;
import com.sfood.automation.goods.application.dto.GoodsStatusLogResponse;
import com.sfood.automation.goods.application.usecase.GetGoodsStatusLogUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GoodsController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class GoodsStatusLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private com.sfood.automation.goods.application.usecase.RegisterGoodsUseCase registerGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.UpdateGoodsUseCase updateGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.UpdateGodoSyncBlockUseCase updateGodoSyncBlockUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.DeleteGoodsUseCase deleteGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCase forceSyncGodoStatusUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.GetGoodsUseCase getGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.GetGoodsListUseCase getGoodsListUseCase;
    @MockBean private GetGoodsStatusLogUseCase getGoodsStatusLogUseCase;

    @Test
    void should_return_unauthorized_without_session() throws Exception {
        mockMvc.perform(get("/api/goods/status-logs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_goods_status_logs() throws Exception {
        when(getGoodsStatusLogUseCase.execute("10000001", "2026-03-31T00:00:00Z", "2026-03-31T23:59:59Z"))
            .thenReturn(new GoodsStatusLogListResponse(List.of(
                new GoodsStatusLogResponse(
                    1,
                    "10000001",
                    "ERP-0001",
                    15,
                    20,
                    50,
                    75,
                    "ON_SALE",
                    "OUT_OF_STOCK",
                    "2026-03-31T12:00:00Z",
                    "2026-03-31T12:00:00Z"
                )
            )));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(get("/api/goods/status-logs")
                .param("godoGoodsCode", "10000001")
                .param("changedFrom", "2026-03-31T00:00:00Z")
                .param("changedTo", "2026-03-31T23:59:59Z")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].godoGoodsCode").value("10000001"))
            .andExpect(jsonPath("$.items[0].afterStatus").value("OUT_OF_STOCK"));
    }
}
