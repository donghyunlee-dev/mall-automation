package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.goods.application.dto.CommonStatusConfigResponse;
import com.sfood.automation.goods.application.dto.UpdateCommonStatusConfigRequest;
import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.CommonStatusConfig;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCommonStatusConfigUseCase {

    private final CommonStatusConfigRepository repository;

    public UpdateCommonStatusConfigUseCase(CommonStatusConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CommonStatusConfigResponse execute(GoodsScope scope, UpdateCommonStatusConfigRequest request, String sessionEmail) {
        var existing = repository.findByScope(scope).orElseThrow();
        var saved = repository.save(new CommonStatusConfig(
            existing.id(),
            existing.scope(),
            request.outOfStockQty(),
            request.onSaleQty(),
            request.safetyRate(),
            request.remainValidDays(),
            sessionEmail,
            Instant.now()
        ));
        return new CommonStatusConfigResponse(
            saved.scope().name(),
            saved.outOfStockQty(),
            saved.onSaleQty(),
            saved.safetyRate(),
            saved.remainValidDays()
        );
    }
}
