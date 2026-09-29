package com.sfood.automation.goods.application.usecase;

import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.model.StatusTransitionResult;
import com.sfood.automation.goods.domain.model.SyncResult;
import com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicy;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import com.sfood.automation.goods.domain.port.GoodsStatusLogRepository;
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import com.sfood.automation.wms.infrastructure.scraping.AggregatedStock;
import com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregator;
import com.sfood.automation.wms.infrastructure.scraping.WmsStockItem;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncGodoStatusUseCase {

    private static final String SYSTEM_USER = "system";

    private final GoodsRepository goodsRepository;
    private final GodoApiPort godoApiPort;
    private final GoodsStatusLogRepository goodsStatusLogRepository;
    private final WmsStockAggregator wmsStockAggregator;
    private final GoodsSoldOutPolicy goodsSoldOutPolicy;
    private final SlackNotifier slackNotifier;
    private final SfoodAdminGoodsPort sfoodAdminGoodsPort;

    public SyncGodoStatusUseCase(
        GoodsRepository goodsRepository,
        GodoApiPort godoApiPort,
        GoodsStatusLogRepository goodsStatusLogRepository,
        WmsStockAggregator wmsStockAggregator,
        GoodsSoldOutPolicy goodsSoldOutPolicy,
        SlackNotifier slackNotifier,
        SfoodAdminGoodsPort sfoodAdminGoodsPort
    ) {
        this.goodsRepository = goodsRepository;
        this.godoApiPort = godoApiPort;
        this.goodsStatusLogRepository = goodsStatusLogRepository;
        this.wmsStockAggregator = wmsStockAggregator;
        this.goodsSoldOutPolicy = goodsSoldOutPolicy;
        this.slackNotifier = slackNotifier;
        this.sfoodAdminGoodsPort = sfoodAdminGoodsPort;
    }

    @Transactional
    public SyncResult execute(List<WmsStockItem> refinedStockItems) {
        Map<String, AggregatedStock> aggregatedStocks = wmsStockAggregator.aggregate(refinedStockItems);
        return execute(aggregatedStocks, aggregatedStocks);
    }

    @Transactional
    public SyncResult execute(Map<String, AggregatedStock> soldOutStocks, Map<String, AggregatedStock> onSaleStocks) {
        java.util.ArrayList<GoodsStatusLog> changedLogs = new java.util.ArrayList<>();
        List<Goods> outOfStockCandidates = goodsRepository.findAll("N", null, "NORMAL", null);
        List<Goods> onSaleCandidates = goodsRepository.findAll("Y", null, "NORMAL", null);

        int outOfStockCount = process(outOfStockCandidates, soldOutStocks, changedLogs);
        int onSaleCount = process(onSaleCandidates, onSaleStocks, changedLogs);

        return new SyncResult(outOfStockCount, onSaleCount, List.copyOf(changedLogs));
    }

    @Transactional
    public SyncResult executeByGoods(Map<Integer, AggregatedStock> soldOutStocksByGoodsId, Map<Integer, AggregatedStock> onSaleStocksByGoodsId) {
        java.util.ArrayList<GoodsStatusLog> changedLogs = new java.util.ArrayList<>();
        List<Goods> outOfStockCandidates = goodsRepository.findAll("N", null, "NORMAL", null);
        List<Goods> onSaleCandidates = goodsRepository.findAll("Y", null, "NORMAL", null);

        int outOfStockCount = processByGoods(outOfStockCandidates, soldOutStocksByGoodsId, changedLogs);
        int onSaleCount = processByGoods(onSaleCandidates, onSaleStocksByGoodsId, changedLogs);

        return new SyncResult(outOfStockCount, onSaleCount, List.copyOf(changedLogs));
    }

    private int process(List<Goods> goodsList, Map<String, AggregatedStock> aggregatedStocks, List<GoodsStatusLog> changedLogs) {
        int changedCount = 0;
        for (Goods goods : goodsList) {
            if ("Y".equals(goods.getGodoSyncBlockedYn())) {
                continue;
            }
            AggregatedStock aggregatedStock = aggregatedStocks.get(goods.getErpGoodsCode());
            StatusTransitionResult result = goodsSoldOutPolicy.evaluate(goods, aggregatedStock);
            if (result == StatusTransitionResult.SET_OUT_OF_STOCK) {
                changedCount += applyTransition(goods, aggregatedStock, "y", "Y", "ON_SALE", "OUT_OF_STOCK", goods.getAvailableQty(), changedLogs);
            } else if (result == StatusTransitionResult.SET_ON_SALE) {
                changedCount += applyTransition(
                    goods,
                    aggregatedStock,
                    "n",
                    "N",
                    "OUT_OF_STOCK",
                    "ON_SALE",
                    goods.getAvailableQty(),
                    changedLogs
                );
            }
        }
        return changedCount;
    }

    private int processByGoods(List<Goods> goodsList, Map<Integer, AggregatedStock> aggregatedStocksByGoodsId, List<GoodsStatusLog> changedLogs) {
        int changedCount = 0;
        for (Goods goods : goodsList) {
            if ("Y".equals(goods.getGodoSyncBlockedYn())) {
                continue;
            }
            AggregatedStock aggregatedStock = goods.getId() == null ? null : aggregatedStocksByGoodsId.get(goods.getId());
            StatusTransitionResult result = goodsSoldOutPolicy.evaluate(goods, aggregatedStock);
            if (result == StatusTransitionResult.SET_OUT_OF_STOCK) {
                changedCount += applyTransition(goods, aggregatedStock, "y", "Y", "ON_SALE", "OUT_OF_STOCK", goods.getAvailableQty(), changedLogs);
            } else if (result == StatusTransitionResult.SET_ON_SALE) {
                changedCount += applyTransition(
                    goods,
                    aggregatedStock,
                    "n",
                    "N",
                    "OUT_OF_STOCK",
                    "ON_SALE",
                    goods.getAvailableQty(),
                    changedLogs
                );
            }
        }
        return changedCount;
    }

    private int applyTransition(
        Goods goods,
        AggregatedStock aggregatedStock,
        String soldOutFl,
        String soldOutYn,
        String beforeStatus,
        String afterStatus,
        int criteriaQty,
        List<GoodsStatusLog> changedLogs
    ) {
        try {
            var result = callStatusApi(goods.getGodoGoodsCode(), soldOutFl);
            if (!result.success()) {
                slackNotifier.send(
                    "SyncGodoStatusUseCase.applyTransition",
                    "Failed to update sold-out status for godoGoodsCode=" + goods.getGodoGoodsCode()
                );
                return 0;
            }
        } catch (GodoApiException | SfoodAdminException exception) {
            slackNotifier.send(
                "SyncGodoStatusUseCase.applyTransition",
                "Godo API error for godoGoodsCode=" + goods.getGodoGoodsCode()
            );
            return 0;
        }

        goods.updateSoldOutStatus(soldOutYn, SYSTEM_USER);
        goodsRepository.save(goods);
        changedLogs.add(saveLog(goods, aggregatedStock, beforeStatus, afterStatus, criteriaQty));
        return 1;
    }

    private GodoApiResult callStatusApi(String godoGoodsCode, String soldOutFl) {
        if ("n".equals(soldOutFl)) {
            return sfoodAdminGoodsPort.restoreToOnSale(godoGoodsCode);
        }
        return godoApiPort.updateSoldOutStatus(godoGoodsCode, soldOutFl);
    }

    private GoodsStatusLog saveLog(
        Goods goods,
        AggregatedStock aggregatedStock,
        String beforeStatus,
        String afterStatus,
        int criteriaQty
    ) {
        String now = Instant.now().toString();
        Integer validPercent = aggregatedStock == null ? Integer.valueOf(0) : aggregatedStock.validPercent();
        Integer remainValidDays = aggregatedStock == null ? Integer.valueOf(0) : aggregatedStock.remainValidDays();
        return goodsStatusLogRepository.save(new GoodsStatusLog(
            null,
            goods.getGodoGoodsCode(),
            goods.getErpGoodsCode(),
            aggregatedStock == null ? 0 : aggregatedStock.totalInvnQty(),
            criteriaQty,
            goods.getSafetyRate(),
            validPercent,
            remainValidDays,
            beforeStatus,
            afterStatus,
            now,
            now
        ));
    }
}
