package com.sfood.automation.batch.infrastructure.job;

import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCase;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.SyncResult;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import com.sfood.automation.wms.infrastructure.scraping.AggregatedStock;
import com.sfood.automation.wms.infrastructure.scraping.ErpCodeVerifier;
import com.sfood.automation.wms.infrastructure.scraping.ErpVerifyResult;
import com.sfood.automation.wms.infrastructure.scraping.StockDataRefiner;
import com.sfood.automation.wms.infrastructure.scraping.WmsLoginClient;
import com.sfood.automation.wms.infrastructure.scraping.WmsStockClient;
import com.sfood.automation.wms.infrastructure.scraping.WmsStockItem;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductSyncBatchJob {

    private final WmsLoginClient wmsLoginClient;
    private final WmsStockClient wmsStockClient;
    private final GoodsRepository goodsRepository;
    private final ErpCodeVerifier erpCodeVerifier;
    private final StockDataRefiner stockDataRefiner;
    private final SyncGodoStatusUseCase syncGodoStatusUseCase;
    private final BatchResultMailSender batchResultMailSender;
    private final SlackNotifier slackNotifier;

    public ProductSyncBatchJob(
        WmsLoginClient wmsLoginClient,
        WmsStockClient wmsStockClient,
        GoodsRepository goodsRepository,
        ErpCodeVerifier erpCodeVerifier,
        StockDataRefiner stockDataRefiner,
        SyncGodoStatusUseCase syncGodoStatusUseCase,
        BatchResultMailSender batchResultMailSender,
        SlackNotifier slackNotifier
    ) {
        this.wmsLoginClient = wmsLoginClient;
        this.wmsStockClient = wmsStockClient;
        this.goodsRepository = goodsRepository;
        this.erpCodeVerifier = erpCodeVerifier;
        this.stockDataRefiner = stockDataRefiner;
        this.syncGodoStatusUseCase = syncGodoStatusUseCase;
        this.batchResultMailSender = batchResultMailSender;
        this.slackNotifier = slackNotifier;
    }

    @Transactional
    public String run() {
        HttpCookie cookie = wmsLoginClient.login();
        List<WmsStockItem> stockItems = wmsStockClient.fetchAll(cookie);

        List<Goods> allGoods = goodsRepository.findAll(null, null, null, null);
        List<Goods> erpSyncNGoods = goodsRepository.findAllErpSyncNGoods();
        List<Goods> alreadySyncedGoods = allGoods.stream()
            .filter(goods -> "Y".equals(goods.getErpSyncYn()))
            .toList();

        List<ErpVerifyResult> verifyResults = erpCodeVerifier.verify(stockItems, erpSyncNGoods, alreadySyncedGoods);
        applyErpVerification(verifyResults);

        List<Goods> goodsForSync = goodsRepository.findAll(null, null, null, null);
        Map<Integer, AggregatedStock> soldOutStocks = stockDataRefiner.refineByGoods(stockItems, goodsForSync);
        Map<Integer, AggregatedStock> onSaleStocks = stockDataRefiner.refineForOnSaleByGoods(stockItems, goodsForSync);

        SyncResult syncResult = syncGodoStatusUseCase.executeByGoods(soldOutStocks, onSaleStocks);
        batchResultMailSender.send(syncResult.changedLogs());
        return "품절 " + syncResult.outOfStockCount() + "건, 정상 전환 " + syncResult.onSaleCount() + "건";
    }

    private void applyErpVerification(List<ErpVerifyResult> verifyResults) {
        for (ErpVerifyResult verifyResult : verifyResults) {
            goodsRepository.findById(verifyResult.goodsId()).ifPresent(goods -> {
                if (com.sfood.automation.goods.domain.enums.GoodsStatus.HIDE.equals(goods.getStatus())) {
                    return;
                }
                if ("Y".equals(verifyResult.erpSyncYn())) {
                    goods.recoverErpSync();
                } else {
                    goods.markErpError(verifyResult.message());
                }
                goodsRepository.save(goods);
            });
        }
    }
}
