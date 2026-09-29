package com.sfood.automation.batch.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sfood.automation.common.exception.WmsScrapingException;
import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCase;
import com.sfood.automation.goods.domain.enums.GoodsStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductSyncBatchJobTest {

    @Test
    void should_throw_when_wms_login_fails() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = org.mockito.Mockito.mock(StockDataRefiner.class);
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        when(wmsLoginClient.login()).thenThrow(new WmsScrapingException());

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        assertThatThrownBy(batchJob::run).isInstanceOf(WmsScrapingException.class);
        verify(wmsLoginClient).login();
    }

    @Test
    void should_execute_job_in_expected_order() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = org.mockito.Mockito.mock(StockDataRefiner.class);
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(new WmsStockItem("ERP-0001", "goods", 20, 80, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"));
        Goods syncedGoods = normalGoods(1, "ERP-0001");
        Goods erroredGoods = errorGoods(2, "ERP-0002");
        List<Goods> goods = List.of(syncedGoods, erroredGoods);
        List<Goods> erpSyncNGoods = List.of(syncedGoods);
        List<Goods> alreadySyncedGoods = List.of(syncedGoods);

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(erpSyncNGoods);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(goods);
        when(erpCodeVerifier.verify(stockItems, erpSyncNGoods, alreadySyncedGoods))
            .thenReturn(List.of(new ErpVerifyResult(1, "Y", "NORMAL", null)));
        AggregatedStock refinedStock = new AggregatedStock("ERP-0001", 20, 80, true, true, true);
        when(goodsRepository.findById(1)).thenReturn(java.util.Optional.of(syncedGoods));
        when(stockDataRefiner.refineByGoods(stockItems, goods)).thenReturn(Map.of(1, refinedStock));
        when(stockDataRefiner.refineForOnSaleByGoods(stockItems, goods)).thenReturn(Map.of(1, refinedStock));
        List<com.sfood.automation.goods.domain.model.GoodsStatusLog> changedLogs = List.of(
            new com.sfood.automation.goods.domain.model.GoodsStatusLog(null, "10000001", "ERP-0001", 20, 20, 50, 80, "ON_SALE", "OUT_OF_STOCK", "2026-04-01T00:00:00Z", "2026-04-01T00:00:00Z")
        );
        when(syncGodoStatusUseCase.executeByGoods(Map.of(1, refinedStock), Map.of(1, refinedStock))).thenReturn(new SyncResult(1, 0, changedLogs));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        String message = batchJob.run();

        assertThat(message).isEqualTo("품절 1건, 정상 전환 0건");
        var inOrder = inOrder(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );
        inOrder.verify(wmsLoginClient).login();
        inOrder.verify(wmsStockClient).fetchAll(cookie);
        inOrder.verify(goodsRepository).findAll(null, null, null, null);
        inOrder.verify(goodsRepository).findAllErpSyncNGoods();
        inOrder.verify(erpCodeVerifier).verify(stockItems, erpSyncNGoods, alreadySyncedGoods);
        inOrder.verify(stockDataRefiner).refineByGoods(stockItems, goods);
        inOrder.verify(stockDataRefiner).refineForOnSaleByGoods(stockItems, goods);
        inOrder.verify(syncGodoStatusUseCase).executeByGoods(Map.of(1, refinedStock), Map.of(1, refinedStock));
        inOrder.verify(batchResultMailSender).send(changedLogs);
    }

    @Test
    void should_pass_aggregated_refined_stock_to_sync_use_case() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = new StockDataRefiner(new com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregator());
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(
            new WmsStockItem("ERP-0001", "goods", 10, 80, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("ERP-0001", "goods", 15, 70, "LOT-2", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("ERP-9999", "goods", 99, 60, "LOT-X", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00")
        );
        Goods syncedGoods = normalGoods(1, "ERP-0001");
        List<Goods> goods = List.of(syncedGoods);

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(goods);
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(List.of());
        when(erpCodeVerifier.verify(stockItems, List.of(), goods)).thenReturn(List.of());
        when(syncGodoStatusUseCase.executeByGoods(anyMap(), anyMap())).thenReturn(new SyncResult(0, 0, List.of()));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        batchJob.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> soldOutStocksCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> onSaleStocksCaptor = ArgumentCaptor.forClass(Map.class);
        verify(syncGodoStatusUseCase).executeByGoods(soldOutStocksCaptor.capture(), onSaleStocksCaptor.capture());
        Map<Integer, AggregatedStock> soldOutStocks = soldOutStocksCaptor.getValue();
        Map<Integer, AggregatedStock> onSaleStocks = onSaleStocksCaptor.getValue();

        assertThat(soldOutStocks).hasSize(1);
        assertThat(soldOutStocks).containsOnlyKeys(1);
        assertThat(soldOutStocks.get(1).totalInvnQty()).isEqualTo(25);
        assertThat(soldOutStocks.get(1).minValidPercent()).isEqualTo(80);
        assertThat(soldOutStocks.get(1).lotExists()).isTrue();
        assertThat(onSaleStocks).hasSize(1);
        assertThat(onSaleStocks.get(1).totalInvnQty()).isEqualTo(25);

    }

    @Test
    void should_filter_on_sale_aggregation_by_goods_safety_rate() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = new StockDataRefiner(new com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregator());
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(
            new WmsStockItem("279110073", "goods", 10, 60, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("279110073", "goods", 15, 30, "LOT-2", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00")
        );
        Goods goods = new Goods(
            164,
            "1000000062",
            "279110073",
            "Goods 164",
            "N",
            "Y",
            20,
            40,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(List.of(goods));
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(List.of());
        when(erpCodeVerifier.verify(stockItems, List.of(), List.of(goods))).thenReturn(List.of());
        when(syncGodoStatusUseCase.executeByGoods(anyMap(), anyMap())).thenReturn(new SyncResult(0, 0, List.of()));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        batchJob.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> soldOutStocksCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> onSaleStocksCaptor = ArgumentCaptor.forClass(Map.class);
        verify(syncGodoStatusUseCase).executeByGoods(soldOutStocksCaptor.capture(), onSaleStocksCaptor.capture());

        assertThat(soldOutStocksCaptor.getValue().get(164).totalInvnQty()).isEqualTo(10);
        assertThat(soldOutStocksCaptor.getValue().get(164).minValidPercent()).isEqualTo(60);
        assertThat(onSaleStocksCaptor.getValue().get(164).totalInvnQty()).isEqualTo(10);
        assertThat(onSaleStocksCaptor.getValue().get(164).minValidPercent()).isEqualTo(60);
    }

    @Test
    void should_prepare_different_stock_results_for_duplicate_erp_goods() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = new StockDataRefiner(new com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregator());
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(
            new WmsStockItem("ERP-0001", "goods", 10, 80, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("ERP-0001", "goods", 15, 35, "LOT-2", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00")
        );
        Goods strictGoods = goodsWithCriteria(1, "ERP-0001", 20, 50);
        Goods relaxedGoods = goodsWithCriteria(2, "ERP-0001", 5, 30);
        List<Goods> goods = List.of(strictGoods, relaxedGoods);

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(goods);
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(List.of());
        when(erpCodeVerifier.verify(stockItems, List.of(), goods)).thenReturn(List.of());
        when(syncGodoStatusUseCase.executeByGoods(anyMap(), anyMap())).thenReturn(new SyncResult(0, 0, List.of()));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        batchJob.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> soldOutStocksCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> onSaleStocksCaptor = ArgumentCaptor.forClass(Map.class);
        verify(syncGodoStatusUseCase).executeByGoods(soldOutStocksCaptor.capture(), onSaleStocksCaptor.capture());

        assertThat(soldOutStocksCaptor.getValue().get(1).totalInvnQty()).isEqualTo(10);
        assertThat(soldOutStocksCaptor.getValue().get(2).totalInvnQty()).isEqualTo(25);
        assertThat(onSaleStocksCaptor.getValue().get(1).minValidPercent()).isEqualTo(80);
        assertThat(onSaleStocksCaptor.getValue().get(2).minValidPercent()).isEqualTo(80);
    }

    @Test
    void should_ignore_non_allowed_zone_rows_before_grouping_item_stock() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = new StockDataRefiner(new com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregator());
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(
            new WmsStockItem("279110073", "goods", 38, 47, "LOT-1", "OA110", "43", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("279110073", "goods", 40, 47, "LOT-1", "OA110", "SHP", "", "SLP001", "2026-04-01T00:00:00")
        );
        Goods goods = new Goods(
            236,
            "1000000062",
            "279110073",
            "Goods 236",
            "N",
            "Y",
            20,
            40,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(List.of(goods));
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(List.of());
        when(erpCodeVerifier.verify(stockItems, List.of(), List.of(goods))).thenReturn(List.of());
        when(syncGodoStatusUseCase.executeByGoods(anyMap(), anyMap())).thenReturn(new SyncResult(0, 0, List.of()));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        batchJob.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> soldOutStocksCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> onSaleStocksCaptor = ArgumentCaptor.forClass(Map.class);
        verify(syncGodoStatusUseCase).executeByGoods(soldOutStocksCaptor.capture(), onSaleStocksCaptor.capture());

        assertThat(soldOutStocksCaptor.getValue().get(236).totalInvnQty()).isEqualTo(38);
        assertThat(soldOutStocksCaptor.getValue().get(236).allowedZone()).isTrue();
        assertThat(onSaleStocksCaptor.getValue().get(236).totalInvnQty()).isEqualTo(38);
        assertThat(onSaleStocksCaptor.getValue().get(236).minValidPercent()).isEqualTo(47);
    }

    @Test
    void should_include_b1_zone_rows_in_grouped_item_stock() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = new StockDataRefiner(new com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregator());
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(
            new WmsStockItem("279110073", "goods", 30, 47, "LOT-1", "OA110", "43", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("279110073", "goods", 12, 47, "LOT-2", "OA110", "B1", "", "SLP001", "2026-04-01T00:00:00"),
            new WmsStockItem("279110073", "goods", 40, 47, "LOT-3", "OA110", "SHP", "", "SLP001", "2026-04-01T00:00:00")
        );
        Goods goods = new Goods(
            236,
            "1000000062",
            "279110073",
            "Goods 236",
            "N",
            "Y",
            20,
            40,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(List.of(goods));
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(List.of());
        when(erpCodeVerifier.verify(stockItems, List.of(), List.of(goods))).thenReturn(List.of());
        when(syncGodoStatusUseCase.executeByGoods(anyMap(), anyMap())).thenReturn(new SyncResult(0, 0, List.of()));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        batchJob.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, AggregatedStock>> soldOutStocksCaptor = ArgumentCaptor.forClass(Map.class);
        verify(syncGodoStatusUseCase).executeByGoods(soldOutStocksCaptor.capture(), anyMap());

        assertThat(soldOutStocksCaptor.getValue().get(236).totalInvnQty()).isEqualTo(42);
        assertThat(soldOutStocksCaptor.getValue().get(236).allowedZone()).isTrue();
    }

    @Test
    void should_not_restore_hidden_goods_during_erp_verification() {
        WmsLoginClient wmsLoginClient = org.mockito.Mockito.mock(WmsLoginClient.class);
        WmsStockClient wmsStockClient = org.mockito.Mockito.mock(WmsStockClient.class);
        GoodsRepository goodsRepository = org.mockito.Mockito.mock(GoodsRepository.class);
        ErpCodeVerifier erpCodeVerifier = org.mockito.Mockito.mock(ErpCodeVerifier.class);
        StockDataRefiner stockDataRefiner = org.mockito.Mockito.mock(StockDataRefiner.class);
        SyncGodoStatusUseCase syncGodoStatusUseCase = org.mockito.Mockito.mock(SyncGodoStatusUseCase.class);
        BatchResultMailSender batchResultMailSender = org.mockito.Mockito.mock(BatchResultMailSender.class);
        SlackNotifier slackNotifier = org.mockito.Mockito.mock(SlackNotifier.class);
        HttpCookie cookie = new HttpCookie("JSESSIONID", "cookie");
        List<WmsStockItem> stockItems = List.of(new WmsStockItem("ERP-0001", "goods", 20, 80, "LOT-1", "OA110", "104", "", "SLP001", "2026-04-01T00:00:00"));
        Goods hiddenGoods = hiddenGoods(1, "ERP-0001");

        when(wmsLoginClient.login()).thenReturn(cookie);
        when(wmsStockClient.fetchAll(cookie)).thenReturn(stockItems);
        when(goodsRepository.findAll(null, null, null, null)).thenReturn(List.of());
        when(goodsRepository.findAllErpSyncNGoods()).thenReturn(List.of(hiddenGoods));
        when(erpCodeVerifier.verify(stockItems, List.of(hiddenGoods), List.of())).thenReturn(List.of(new ErpVerifyResult(1, "Y", "NORMAL", null)));
        when(goodsRepository.findById(1)).thenReturn(java.util.Optional.of(hiddenGoods));
        when(stockDataRefiner.refineByGoods(stockItems, List.of())).thenReturn(Map.of());
        when(stockDataRefiner.refineForOnSaleByGoods(stockItems, List.of())).thenReturn(Map.of());
        when(syncGodoStatusUseCase.executeByGoods(Map.of(), Map.of())).thenReturn(new SyncResult(0, 0, List.of()));

        ProductSyncBatchJob batchJob = new ProductSyncBatchJob(
            wmsLoginClient,
            wmsStockClient,
            goodsRepository,
            erpCodeVerifier,
            stockDataRefiner,
            syncGodoStatusUseCase,
            batchResultMailSender,
            slackNotifier
        );

        batchJob.run();

        assertThat(hiddenGoods.getStatus()).isEqualTo(GoodsStatus.HIDE);
        verify(goodsRepository, never()).save(hiddenGoods);
    }

    private Goods normalGoods(int id, String erpCode) {
        return new Goods(
            id,
            "1000000" + id,
            erpCode,
            "Goods " + id,
            "N",
            "N",
            20,
            50,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );
    }

    private Goods errorGoods(int id, String erpCode) {
        return new Goods(
            id,
            "1000000" + id,
            erpCode,
            "Goods " + id,
            "N",
            "N",
            20,
            50,
            150,
            "Y",
            "N",
            null,
            "ERP error",
            GoodsStatus.ERROR,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );
    }

    private Goods hiddenGoods(int id, String erpCode) {
        return new Goods(
            id,
            "1000000" + id,
            erpCode,
            "Goods " + id,
            "N",
            "N",
            20,
            50,
            150,
            "Y",
            "N",
            null,
            "ERP error",
            GoodsStatus.HIDE,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );
    }

    private Goods goodsWithCriteria(int id, String erpCode, int availableQty, int safetyRate) {
        return new Goods(
            id,
            "1000000" + id,
            erpCode,
            "Goods " + id,
            "N",
            "N",
            availableQty,
            safetyRate,
            150,
            "Y",
            "Y",
            null,
            null,
            GoodsStatus.NORMAL,
            "user@s-food.com",
            Instant.parse("2026-04-01T00:00:00Z"),
            null,
            null
        );
    }
}
