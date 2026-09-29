package com.sfood.automation.goods.presentation;

import com.sfood.automation.common.security.SessionAuthInterceptor;
import com.sfood.automation.goods.application.dto.CommonStatusConfigResponse;
import com.sfood.automation.goods.application.dto.UpdateCommonStatusConfigRequest;
import com.sfood.automation.goods.application.usecase.GetCommonStatusConfigUseCase;
import com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCase;
import com.sfood.automation.goods.domain.enums.GoodsScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/status")
public class CommonStatusConfigController {

    private final GetCommonStatusConfigUseCase getUseCase;
    private final UpdateCommonStatusConfigUseCase updateUseCase;

    public CommonStatusConfigController(GetCommonStatusConfigUseCase getUseCase, UpdateCommonStatusConfigUseCase updateUseCase) {
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
    }

    @GetMapping
    ResponseEntity<List<CommonStatusConfigResponse>> getConfig() {
        return ResponseEntity.ok(getUseCase.execute());
    }

    @PutMapping("/{scope}")
    ResponseEntity<CommonStatusConfigResponse> updateConfig(
        @PathVariable GoodsScope scope,
        @Valid @RequestBody UpdateCommonStatusConfigRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(updateUseCase.execute(scope, request, sessionEmail(httpServletRequest)));
    }

    private String sessionEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object email = session == null ? null : session.getAttribute(SessionAuthInterceptor.AUTH_SESSION_KEY);
        return email == null ? null : email.toString();
    }
}
