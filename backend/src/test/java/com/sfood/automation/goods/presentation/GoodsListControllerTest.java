package com.sfood.automation.goods.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.goods.application.dto.GoodsListItemResponse;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.goods.application.dto.GoodsListResponse;
import com.sfood.automation.goods.application.usecase.GetGoodsListUseCase;
import com.sfood.automation.goods.application.usecase.GetGoodsStatusLogUseCase;
import java.time.Instant;
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
class GoodsListControllerTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @MockBean private com.sfood.automation.goods.application.usecase.RegisterGoodsUseCase registerGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.UpdateGoodsUseCase updateGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.UpdateGodoSyncBlockUseCase updateGodoSyncBlockUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.DeleteGoodsUseCase deleteGoodsUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCase forceSyncGodoStatusUseCase;
    @MockBean private com.sfood.automation.goods.application.usecase.GetGoodsUseCase getGoodsUseCase;
    @MockBean private GetGoodsListUseCase getGoodsListUseCase;
    @MockBean private GetGoodsStatusLogUseCase getGoodsStatusLogUseCase;

    @Test
    void should_return_goods_list() throws Exception {
        when(getGoodsListUseCase.execute("N", null, null, null))
            .thenReturn(new GoodsListResponse(List.of(
                new GoodsListItemResponse(1, "10000001", "ERP-0001", "Sample", "N", "N", 20, 50, 150, "Y", "Y", "Y", null, "UNVERIFIED",
                    Instant.parse("2026-04-02T10:30:00Z"), "editor@s-food.com")

            )));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(get("/api/goods").param("soldOutYn", "N").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].godoGoodsCode").value("10000001"))
            .andExpect(jsonPath("$.items[0].godoSyncBlockedYn").value("Y"))
            .andExpect(jsonPath("$.items[0].recentUpdatedAt").value("2026-04-02T10:30:00Z"))
            .andExpect(jsonPath("$.items[0].recentUpdatedBy").value("editor@s-food.com"));
    }

    @Test
    void should_return_cors_headers_for_frontend_origin() throws Exception {
        when(getGoodsListUseCase.execute("N", null, null, null))
            .thenReturn(new GoodsListResponse(List.of()));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(get("/api/goods")
                .param("soldOutYn", "N")
                .header("Origin", FRONTEND_ORIGIN)
                .session(session))
            .andExpect(status().isOk())
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
            .andExpect(header().string(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
