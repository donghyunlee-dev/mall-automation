package com.sfood.automation.goods.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfood.automation.common.config.WebMvcConfig;
import com.sfood.automation.common.exception.GoodsSyncBlockValidationException;
import com.sfood.automation.common.exception.GlobalExceptionHandler;
import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusItemResponse;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusResponse;
import com.sfood.automation.goods.application.dto.GoodsResponse;
import com.sfood.automation.goods.application.dto.RegisterGoodsListResponse;
import com.sfood.automation.goods.application.dto.RegisterGoodsResponse;
import com.sfood.automation.goods.application.dto.UpdateGodoSyncBlockRequest;
import com.sfood.automation.goods.application.dto.UpdateGoodsRequest;
import com.sfood.automation.goods.application.usecase.DeleteGoodsUseCase;
import com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCase;
import com.sfood.automation.goods.application.usecase.GetGoodsListUseCase;
import com.sfood.automation.goods.application.usecase.GetGoodsStatusLogUseCase;
import com.sfood.automation.goods.application.usecase.GetGoodsUseCase;
import com.sfood.automation.goods.application.usecase.RegisterGoodsUseCase;
import com.sfood.automation.goods.application.usecase.UpdateGodoSyncBlockUseCase;
import com.sfood.automation.goods.application.usecase.UpdateGoodsUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GoodsController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, SessionAuthInterceptor.class})
class GoodsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterGoodsUseCase registerGoodsUseCase;

    @MockBean
    private UpdateGoodsUseCase updateGoodsUseCase;

    @MockBean
    private UpdateGodoSyncBlockUseCase updateGodoSyncBlockUseCase;

    @MockBean
    private DeleteGoodsUseCase deleteGoodsUseCase;

    @MockBean
    private ForceSyncGodoStatusUseCase forceSyncGodoStatusUseCase;

    @MockBean
    private GetGoodsUseCase getGoodsUseCase;

    @MockBean
    private GetGoodsListUseCase getGoodsListUseCase;

    @MockBean
    private GetGoodsStatusLogUseCase getGoodsStatusLogUseCase;

    @Test
    void should_return_unauthorized_without_session() throws Exception {
        mockMvc.perform(post("/api/goods")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.List.of(java.util.Map.of(
                    "godoGoodsCode", "10000001",
                    "erpGoodsCode", "ERP-0001",
                    "goodsName", "Sample",
                    "onlyMallYn", "N"
                )))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_bad_request_when_godo_goods_code_missing() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        mockMvc.perform(post("/api/goods").session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.List.of(java.util.Map.of(
                    "erpGoodsCode", "ERP-0001",
                    "goodsName", "Sample",
                    "onlyMallYn", "N"
                )))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_created_when_registration_succeeds() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(registerGoodsUseCase.execute(any(), eq("user@s-food.com")))
            .thenReturn(new RegisterGoodsListResponse(java.util.List.of(
                new RegisterGoodsResponse(1, "10000001", "ERP-0001", "UNVERIFIED", null)
            )));

        mockMvc.perform(post("/api/goods").session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(java.util.List.of(java.util.Map.of(
                    "godoGoodsCode", "10000001",
                    "erpGoodsCode", "ERP-0001",
                    "goodsName", "Sample",
                    "onlyMallYn", "N"
                )))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].id").value(1))
            .andExpect(jsonPath("$.items[0].status").value("UNVERIFIED"));
    }

    @Test
    void should_return_ok_when_goods_update_succeeds() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(updateGoodsUseCase.execute(eq(1), any(UpdateGoodsRequest.class), eq("user@s-food.com")))
            .thenReturn(new GoodsResponse(
                1, "10000001", "ERP-0001", "Updated Goods", "Y", "N", 7, 40, 150, "Y", "Y", "N",
                null, null, "UNVERIFIED", "creator@s-food.com", Instant.parse("2026-04-01T09:00:00Z"), null, null
            ));

        mockMvc.perform(put("/api/goods/{id}", 1).session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdateGoodsRequest("Updated Goods", "ERP-0001", "Y", 7, 40, 150))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.goodsName").value("Updated Goods"))
            .andExpect(jsonPath("$.onlyMallYn").value("Y"))
            .andExpect(jsonPath("$.availableQty").value(7))
            .andExpect(jsonPath("$.safetyRate").value(40));
    }

    @Test
    void should_update_godo_sync_block_when_request_is_valid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(updateGodoSyncBlockUseCase.execute(eq(1), any(UpdateGodoSyncBlockRequest.class), eq("user@s-food.com")))
            .thenReturn(new GoodsResponse(
                1, "10000001", "ERP-0001", "Sample Goods", "N", "N", 20, 50, 150, "Y", "Y", "Y",
                null, null, "NORMAL", "creator@s-food.com", Instant.parse("2026-04-01T09:00:00Z"), "user@s-food.com",
                Instant.parse("2026-04-02T10:30:00Z")
            ));

        mockMvc.perform(patch("/api/goods/{id}/godo-sync-block", 1).session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdateGodoSyncBlockRequest("Y"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.godoSyncBlockedYn").value("Y"));
    }

    @Test
    void should_return_bad_request_when_godo_sync_block_request_is_invalid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(patch("/api/goods/{id}/godo-sync-block", 1).session(session)
                .contentType("application/json")
                .content("""
                    {
                      "blockedYn": "BLOCKED"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_when_enabling_godo_sync_block_for_non_normal_goods() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(updateGodoSyncBlockUseCase.execute(eq(1), any(UpdateGodoSyncBlockRequest.class), eq("user@s-food.com")))
            .thenThrow(new GoodsSyncBlockValidationException("Godo sync block can only be enabled for NORMAL goods"));

        mockMvc.perform(patch("/api/goods/{id}/godo-sync-block", 1).session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdateGodoSyncBlockRequest("Y"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Godo sync block can only be enabled for NORMAL goods"));
    }

    @Test
    void should_force_sync_godo_status_when_request_is_valid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(forceSyncGodoStatusUseCase.execute(any(), eq("user@s-food.com")))
            .thenReturn(new ForceSyncGodoStatusResponse(java.util.List.of(
                new ForceSyncGodoStatusItemResponse("10000001", "SUCCESS", null, "Y")
            )));

        mockMvc.perform(post("/api/goods/godo-status/force-sync").session(session)
                .contentType("application/json")
                .content("""
                    {
                      "godoGoodsCodes": ["10000001"],
                      "targetSoldOutYn": "Y"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].godoGoodsCode").value("10000001"))
            .andExpect(jsonPath("$.items[0].result").value("SUCCESS"))
            .andExpect(jsonPath("$.items[0].soldOutYn").value("Y"));
    }

    @Test
    void should_return_bad_request_when_force_sync_request_is_invalid() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(post("/api/goods/godo-status/force-sync").session(session)
                .contentType("application/json")
                .content("""
                    {
                      "godoGoodsCodes": [],
                      "targetSoldOutYn": "BLOCKED"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_when_goods_update_contains_negative_criteria() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(put("/api/goods/{id}", 1).session(session)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdateGoodsRequest("Updated Goods", "ERP-0001", "Y", -1, 40, 150))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_when_goods_update_omits_available_qty() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(put("/api/goods/{id}", 1).session(session)
                .contentType("application/json")
                .content("""
                    {
                      "goodsName": "Updated Goods",
                      "erpGoodsCode": "ERP-0001",
                      "onlyMallYn": "Y",
                      "safetyRate": 40
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_bad_request_when_goods_update_contains_null_safety_rate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");

        mockMvc.perform(put("/api/goods/{id}", 1).session(session)
                .contentType("application/json")
                .content("""
                    {
                      "goodsName": "Updated Goods",
                      "erpGoodsCode": "ERP-0001",
                      "onlyMallYn": "Y",
                      "availableQty": 7,
                      "safetyRate": null
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_goods_detail_with_full_goods_fields() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY, "user@s-food.com");
        when(getGoodsUseCase.execute(1))
            .thenReturn(new GoodsResponse(
                1,
                "10000001",
                "ERP-0001",
                "Sample Goods",
                "N",
                "N",
                20,
                50,
                150,
                "Y",
                "Y",
                "N",
                "detail note",
                "sync message",
                "NORMAL",
                "creator@s-food.com",
                Instant.parse("2026-04-01T09:00:00Z"),
                "editor@s-food.com",
                Instant.parse("2026-04-02T10:30:00Z")
            ));

        mockMvc.perform(get("/api/goods/{id}", 1).session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description").value("detail note"))
            .andExpect(jsonPath("$.message").value("sync message"))
            .andExpect(jsonPath("$.createdId").value("creator@s-food.com"))
            .andExpect(jsonPath("$.createdAt").value("2026-04-01T09:00:00Z"))
            .andExpect(jsonPath("$.updatedId").value("editor@s-food.com"))
            .andExpect(jsonPath("$.updatedAt").value("2026-04-02T10:30:00Z"));
    }
}
