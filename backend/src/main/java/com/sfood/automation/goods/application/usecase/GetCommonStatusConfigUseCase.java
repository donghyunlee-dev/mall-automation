package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.goods.application.dto.CommonStatusConfigResponse;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetCommonStatusConfigUseCase {

    private final CommonStatusConfigRepository repository;

    public GetCommonStatusConfigUseCase(CommonStatusConfigRepository repository) {
        this.repository = repository;
    }

    public List<CommonStatusConfigResponse> execute() {
        return repository.findAll().stream()
            .map(config -> new CommonStatusConfigResponse(
                config.scope().name(),
                config.outOfStockQty(),
                config.onSaleQty(),
                config.safetyRate(),
                config.remainValidDays()
            ))
            .toList();
    }
}
