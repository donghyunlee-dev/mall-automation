package com.sfood.automation.goods.presentation;

import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.goods.application.dto.GoodsResponse;
import com.sfood.automation.goods.application.dto.GoodsListResponse;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusRequest;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusResponse;
import com.sfood.automation.goods.application.dto.RegisterGoodsRequest;
import com.sfood.automation.goods.application.dto.RegisterGoodsListResponse;
import com.sfood.automation.goods.application.dto.UpdateGodoSyncBlockRequest;
import com.sfood.automation.goods.application.dto.UpdateGoodsRequest;
import com.sfood.automation.goods.application.usecase.DeleteGoodsUseCase;
import com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCase;
import com.sfood.automation.goods.application.usecase.GetGoodsUseCase;
import com.sfood.automation.goods.application.usecase.GetGoodsListUseCase;
import com.sfood.automation.goods.application.dto.GoodsStatusLogListResponse;
import com.sfood.automation.goods.application.usecase.GetGoodsStatusLogUseCase;
import com.sfood.automation.goods.application.usecase.RegisterGoodsUseCase;
import com.sfood.automation.goods.application.usecase.UpdateGodoSyncBlockUseCase;
import com.sfood.automation.goods.application.usecase.UpdateGoodsUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final RegisterGoodsUseCase registerGoodsUseCase;
    private final UpdateGoodsUseCase updateGoodsUseCase;
    private final UpdateGodoSyncBlockUseCase updateGodoSyncBlockUseCase;
    private final DeleteGoodsUseCase deleteGoodsUseCase;
    private final ForceSyncGodoStatusUseCase forceSyncGodoStatusUseCase;
    private final GetGoodsUseCase getGoodsUseCase;
    private final GetGoodsListUseCase getGoodsListUseCase;
    private final GetGoodsStatusLogUseCase getGoodsStatusLogUseCase;

    public GoodsController(
        RegisterGoodsUseCase registerGoodsUseCase,
        UpdateGoodsUseCase updateGoodsUseCase,
        UpdateGodoSyncBlockUseCase updateGodoSyncBlockUseCase,
        DeleteGoodsUseCase deleteGoodsUseCase,
        ForceSyncGodoStatusUseCase forceSyncGodoStatusUseCase,
        GetGoodsUseCase getGoodsUseCase,
        GetGoodsListUseCase getGoodsListUseCase,
        GetGoodsStatusLogUseCase getGoodsStatusLogUseCase
    ) {
        this.registerGoodsUseCase = registerGoodsUseCase;
        this.updateGoodsUseCase = updateGoodsUseCase;
        this.updateGodoSyncBlockUseCase = updateGodoSyncBlockUseCase;
        this.deleteGoodsUseCase = deleteGoodsUseCase;
        this.forceSyncGodoStatusUseCase = forceSyncGodoStatusUseCase;
        this.getGoodsUseCase = getGoodsUseCase;
        this.getGoodsListUseCase = getGoodsListUseCase;
        this.getGoodsStatusLogUseCase = getGoodsStatusLogUseCase;
    }

    @PostMapping
    ResponseEntity<RegisterGoodsListResponse> registerGoods(
        @Valid @RequestBody RegisterGoodsRequest[] requests,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(registerGoodsUseCase.execute(Arrays.asList(requests), sessionEmail(httpServletRequest)));
    }

    @PutMapping("/{id}")
    ResponseEntity<GoodsResponse> updateGoods(@PathVariable Integer id, @Valid @RequestBody UpdateGoodsRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(updateGoodsUseCase.execute(id, request, sessionEmail(httpServletRequest)));
    }

    @PatchMapping("/{id}/godo-sync-block")
    ResponseEntity<GoodsResponse> updateGodoSyncBlock(
        @PathVariable Integer id,
        @Valid @RequestBody UpdateGodoSyncBlockRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(updateGodoSyncBlockUseCase.execute(id, request, sessionEmail(httpServletRequest)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteGoods(@PathVariable Integer id) {
        deleteGoodsUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/godo-status/force-sync")
    ResponseEntity<ForceSyncGodoStatusResponse> forceSyncGodoStatus(
        @Valid @RequestBody ForceSyncGodoStatusRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(forceSyncGodoStatusUseCase.execute(request, sessionEmail(httpServletRequest)));
    }

    @GetMapping("/{id}")
    ResponseEntity<GoodsResponse> getGoods(@PathVariable Integer id) {
        return ResponseEntity.ok(getGoodsUseCase.execute(id));
    }

    @GetMapping
    ResponseEntity<GoodsListResponse> getGoodsList(
        @org.springframework.web.bind.annotation.RequestParam(required = false) String soldOutYn,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String onlyMallYn,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(getGoodsListUseCase.execute(soldOutYn, onlyMallYn, status, keyword));
    }

    @GetMapping("/status-logs")
    ResponseEntity<GoodsStatusLogListResponse> getStatusLogs(
        @org.springframework.web.bind.annotation.RequestParam(required = false) String godoGoodsCode,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String changedFrom,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String changedTo
    ) {
        return ResponseEntity.ok(getGoodsStatusLogUseCase.execute(godoGoodsCode, changedFrom, changedTo));
    }

    private String sessionEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object email = session == null ? null : session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
        return email == null ? null : email.toString();
    }
}
