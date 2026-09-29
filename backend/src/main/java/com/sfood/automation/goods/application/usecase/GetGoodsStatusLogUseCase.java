package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.goods.application.dto.GoodsStatusLogListResponse;
import com.sfood.automation.goods.application.dto.GoodsStatusLogResponse;
import com.sfood.automation.goods.domain.port.GoodsStatusLogRepository;
import org.springframework.stereotype.Service;

@Service
public class GetGoodsStatusLogUseCase {

    private final GoodsStatusLogRepository goodsStatusLogRepository;

    public GetGoodsStatusLogUseCase(GoodsStatusLogRepository goodsStatusLogRepository) {
        this.goodsStatusLogRepository = goodsStatusLogRepository;
    }

    public GoodsStatusLogListResponse execute(String godoGoodsCode, String changedFrom, String changedTo) {
        return new GoodsStatusLogListResponse(
            goodsStatusLogRepository.findAll(godoGoodsCode, changedFrom, changedTo).stream()
                .map(log -> new GoodsStatusLogResponse(
                    log.id(),
                    log.godoGoodsCode(),
                    log.erpGoodsCode(),
                    log.wmsAvailableQty(),
                    log.criteriaQty(),
                    log.safetyRate(),
                    log.validPercent(),
                    log.remainValidDays(),
                    log.beforeStatus(),
                    log.afterStatus(),
                    log.changedAt(),
                    log.createdAt()
                ))
                .toList()
        );
    }
}
