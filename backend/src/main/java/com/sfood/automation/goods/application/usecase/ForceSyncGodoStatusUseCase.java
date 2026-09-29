package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusItemResponse;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusRequest;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusResponse;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import com.sfood.automation.goods.domain.port.GoodsStatusLogRepository;
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForceSyncGodoStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(ForceSyncGodoStatusUseCase.class);
    private static final String FALLBACK_UPDATED_BY = "system";

    private final GoodsRepository goodsRepository;
    private final GodoApiPort godoApiPort;
    private final GoodsStatusLogRepository goodsStatusLogRepository;
    private final SfoodAdminGoodsPort sfoodAdminGoodsPort;

    public ForceSyncGodoStatusUseCase(
        GoodsRepository goodsRepository,
        GodoApiPort godoApiPort,
        GoodsStatusLogRepository goodsStatusLogRepository,
        SfoodAdminGoodsPort sfoodAdminGoodsPort
    ) {
        this.goodsRepository = goodsRepository;
        this.godoApiPort = godoApiPort;
        this.goodsStatusLogRepository = goodsStatusLogRepository;
        this.sfoodAdminGoodsPort = sfoodAdminGoodsPort;
    }

    @Transactional
    public ForceSyncGodoStatusResponse execute(ForceSyncGodoStatusRequest request, String sessionEmail) {
        log.info(
            "Force sync request started: goodsCount={}, targetSoldOutYn={}, sessionEmail={}",
            request.godoGoodsCodes().size(),
            request.targetSoldOutYn(),
            sessionEmail
        );
        List<ForceSyncGodoStatusItemResponse> items = new ArrayList<>();
        for (String godoGoodsCode : request.godoGoodsCodes()) {
            items.add(forceSync(godoGoodsCode, request.targetSoldOutYn(), sessionEmail));
        }
        log.info(
            "Force sync request completed: goodsCount={}, targetSoldOutYn={}",
            items.size(),
            request.targetSoldOutYn()
        );
        return new ForceSyncGodoStatusResponse(List.copyOf(items));
    }

    private ForceSyncGodoStatusItemResponse forceSync(String godoGoodsCode, String targetSoldOutYn, String sessionEmail) {
        Goods goods = goodsRepository.findByGodoCode(godoGoodsCode).orElse(null);
        if (goods == null) {
            log.warn("Force sync skipped: godoGoodsCode={} reason=NOT_FOUND targetSoldOutYn={}", godoGoodsCode, targetSoldOutYn);
            return new ForceSyncGodoStatusItemResponse(godoGoodsCode, "NOT_FOUND", "Goods not found", null);
        }

        log.info(
            "Force sync item started: godoGoodsCode={}, targetSoldOutYn={}, status={}, godoSyncYn={}, godoSyncBlockedYn={}",
            godoGoodsCode,
            targetSoldOutYn,
            goods.getStatus(),
            goods.getGodoSyncYn(),
            goods.getGodoSyncBlockedYn()
        );

        if (!"Y".equals(goods.getGodoSyncYn())) {
            log.warn("Force sync skipped: godoGoodsCode={} reason=GODO_SYNC_DISABLED", godoGoodsCode);
            return skipped(goods, "Godo sync must be Y");
        }

        if ("Y".equals(goods.getGodoSyncBlockedYn())) {
            log.warn("Force sync skipped: godoGoodsCode={} reason=GODO_SYNC_BLOCKED", godoGoodsCode);
            return skipped(goods, "Godo sync is blocked");
        }

        try {
            var result = callStatusApi(goods.getGodoGoodsCode(), targetSoldOutYn);
            if (!result.success()) {
                log.warn(
                    "Force sync failed: godoGoodsCode={} reason=API_NON_SUCCESS resultCode={} resultMessage={}",
                    godoGoodsCode,
                    result.resultCode(),
                    result.resultMessage()
                );
                return new ForceSyncGodoStatusItemResponse(goods.getGodoGoodsCode(), "FAILED", result.resultMessage(), goods.getSoldOutYn());
            }
        } catch (GodoApiException | SfoodAdminException exception) {
            log.error(
                "Force sync failed: godoGoodsCode={} reason=API_EXCEPTION targetSoldOutYn={} currentSoldOutYn={}",
                godoGoodsCode,
                targetSoldOutYn,
                goods.getSoldOutYn(),
                exception
            );
            return new ForceSyncGodoStatusItemResponse(goods.getGodoGoodsCode(), "FAILED", "API error", goods.getSoldOutYn());
        }

        String beforeStatus = soldOutStatus(goods.getSoldOutYn());
        goods.updateSoldOutStatus(targetSoldOutYn, sessionEmail != null ? sessionEmail : FALLBACK_UPDATED_BY);
        goodsRepository.save(goods);
        goodsStatusLogRepository.save(buildStatusLog(goods, beforeStatus, soldOutStatus(targetSoldOutYn)));
        log.info(
            "Force sync succeeded: godoGoodsCode={} beforeStatus={} afterStatus={} updatedBy={}",
            godoGoodsCode,
            beforeStatus,
            soldOutStatus(targetSoldOutYn),
            sessionEmail != null ? sessionEmail : FALLBACK_UPDATED_BY
        );
        return new ForceSyncGodoStatusItemResponse(goods.getGodoGoodsCode(), "SUCCESS", null, goods.getSoldOutYn());
    }

    private ForceSyncGodoStatusItemResponse skipped(Goods goods, String message) {
        return new ForceSyncGodoStatusItemResponse(goods.getGodoGoodsCode(), "SKIPPED", message, goods.getSoldOutYn());
    }

    private GoodsStatusLog buildStatusLog(Goods goods, String beforeStatus, String afterStatus) {
        String now = Instant.now().toString();
        return new GoodsStatusLog(
            null,
            goods.getGodoGoodsCode(),
            goods.getErpGoodsCode(),
            0,
            goods.getAvailableQty(),
            goods.getSafetyRate(),
            0,
            null,
            beforeStatus,
            afterStatus,
            now,
            now
        );
    }

    private com.sfood.automation.goods.domain.model.GodoApiResult callStatusApi(String godoGoodsCode, String targetSoldOutYn) {
        if ("N".equals(targetSoldOutYn)) {
            return sfoodAdminGoodsPort.restoreToOnSale(godoGoodsCode);
        }
        return godoApiPort.updateSoldOutStatus(godoGoodsCode, soldOutFlag(targetSoldOutYn));
    }

    private String soldOutFlag(String targetSoldOutYn) {
        return "Y".equals(targetSoldOutYn) ? "y" : "n";
    }

    private String soldOutStatus(String soldOutYn) {
        return "Y".equals(soldOutYn) ? "OUT_OF_STOCK" : "ON_SALE";
    }
}
