package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.common.slack.SlackNotifier;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsFixtures;
import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.model.SyncResult;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import com.sfood.automation.goods.domain.port.GoodsStatusLogRepository;
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicy;
import com.sfood.automation.wms.infrastructure.scraping.AggregatedStock;
import com.sfood.automation.wms.infrastructure.scraping.WmsStockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SyncGodoStatusUseCaseTest {

    @Test
    void should_call_api_three_times_for_out_of_stock_candidates() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(
            goods("10000001", "ERP-0001", "N", "N"),
            goods("10000002", "ERP-0002", "N", "N"),
            goods("10000003", "ERP-0003", "N", "N")
        );
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository());

        useCase.execute(soldOutStocks(
            stock("ERP-0001", 20, 80),
            stock("ERP-0002", 20, 80),
            stock("ERP-0003", 20, 80)
        ), Map.of());

        assertThat(godoApiPort.calls).hasSize(3);
        assertThat(godoApiPort.calls).containsExactly("10000001:y", "10000002:y", "10000003:y");
    }

    @Test
    void should_update_goods_to_y_when_api_succeeds() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository());

        useCase.execute(soldOutStocks(stock("ERP-0001", 20, 80)), Map.of());

        assertThat(goodsRepository.savedGoods).hasSize(1);
        assertThat(goodsRepository.savedGoods.get(0).getSoldOutYn()).isEqualTo("Y");
    }

    @Test
    void should_not_change_goods_when_api_fails() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        godoApiPort.failForCode("10000001");
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository());

        useCase.execute(soldOutStocks(stock("ERP-0001", 20, 80)), Map.of());

        assertThat(goodsRepository.savedGoods).isEmpty();
    }

    @Test
    void should_save_status_log_when_transition_occurs() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), logRepository);

        useCase.execute(soldOutStocks(stock("ERP-0001", 20, 80)), Map.of());

        assertThat(logRepository.savedLogs).hasSize(1);
        assertThat(logRepository.savedLogs.get(0).wmsAvailableQty()).isEqualTo(20);
        assertThat(logRepository.savedLogs.get(0).criteriaQty()).isEqualTo(20);
    }

    @Test
    void should_set_out_of_stock_when_refined_stock_is_missing() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, logRepository);

        SyncResult result = useCase.execute(Map.of(), Map.of());

        assertThat(result.outOfStockCount()).isEqualTo(1);
        assertThat(godoApiPort.calls).containsExactly("10000001:y");
        assertThat(logRepository.savedLogs.get(0).wmsAvailableQty()).isZero();
        assertThat(logRepository.savedLogs.get(0).validPercent()).isEqualTo(0);
        assertThat(logRepository.savedLogs.get(0).remainValidDays()).isEqualTo(0);
    }

    @Test
    void should_not_call_api_for_no_change_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository());

        useCase.execute(soldOutStocks(stock("ERP-0001", 21, 80)), Map.of());

        assertThat(godoApiPort.calls).isEmpty();
    }

    @Test
    void should_process_out_of_stock_before_on_sale() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(
            goods("10000001", "ERP-0001", "N", "N"),
            goods("10000002", "ERP-0002", "N", "Y")
        );
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

        useCase.execute(
            soldOutStocks(stock("ERP-0001", 20, 80)),
            onSaleStocks(stock("ERP-0002", 21, 50))
        );

        assertThat(godoApiPort.calls).containsExactly("10000001:y");
        assertThat(sfoodAdminGoodsPort.calls).containsExactly("10000002");
    }

    @Test
    void should_return_sync_result_counts() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(
            goods("10000001", "ERP-0001", "N", "N"),
            goods("10000002", "ERP-0002", "N", "Y")
        );
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

        SyncResult result = useCase.execute(
            soldOutStocks(stock("ERP-0001", 20, 80)),
            onSaleStocks(stock("ERP-0002", 21, 50))
        );

        assertThat(result.outOfStockCount()).isEqualTo(1);
        assertThat(result.onSaleCount()).isEqualTo(1);
        assertThat(result.changedLogs()).hasSize(2);
    }

    @Test
    void should_not_call_api_for_error_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(errorGoods("10000001", "ERP-0001", "N", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository());

        useCase.execute(soldOutStocks(stock("ERP-0001", 20, 80)), Map.of());

        assertThat(godoApiPort.calls).isEmpty();
    }

    @Test
    void should_not_call_api_for_godo_sync_blocked_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(blockedGoods("10000001", "ERP-0001", "N", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository());

        useCase.execute(soldOutStocks(stock("ERP-0001", 20, 80)), Map.of());

        assertThat(godoApiPort.calls).isEmpty();
    }

    @Test
    void should_use_refined_aggregated_stock_directly() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), logRepository);

        SyncResult result = useCase.execute(
            soldOutStocks(stock("ERP-0001", 20, 80)),
            Map.of()
        );

        assertThat(result.outOfStockCount()).isEqualTo(1);
        assertThat(logRepository.savedLogs).hasSize(1);
        assertThat(logRepository.savedLogs.get(0).wmsAvailableQty()).isEqualTo(20);
    }

    @Test
    void should_recover_on_sale_using_goods_available_qty_after_safety_filtered_aggregation() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000002", "ERP-0002", "N", "Y"));
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

        SyncResult result = useCase.execute(
            Map.of(),
            onSaleStocks(stock("ERP-0002", 21, 60))
        );

        assertThat(result.onSaleCount()).isEqualTo(1);
        assertThat(sfoodAdminGoodsPort.calls).containsExactly("10000002");
    }

    @Test
    void should_not_flip_same_goods_back_to_on_sale_within_single_batch() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort);

        SyncResult result = useCase.execute(
            soldOutStocks(stock("ERP-0001", 20, 80)),
            onSaleStocks(stock("ERP-0001", 20, 80))
        );

        assertThat(result.outOfStockCount()).isEqualTo(1);
        assertThat(result.onSaleCount()).isZero();
        assertThat(godoApiPort.calls).containsExactly("10000001:y");
        assertThat(sfoodAdminGoodsPort.calls).isEmpty();
        assertThat(goodsRepository.savedGoods.getLast().getSoldOutYn()).isEqualTo("Y");
    }

    @Test
    void should_evaluate_duplicate_erp_goods_independently_when_stock_is_keyed_by_goods_id() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(
            goods("10000001", "ERP-0001", "N", "N"),
            goods("10000002", "ERP-0001", "N", "N")
        );
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        SyncGodoStatusUseCase useCase = useCase(goodsRepository, godoApiPort, new FakeGoodsStatusLogRepository());

        SyncResult result = useCase.executeByGoods(
            Map.of(
                1, stock("ERP-0001", 10, 80),
                2, stock("ERP-0001", 25, 35)
            ),
            Map.of()
        );

        assertThat(result.outOfStockCount()).isEqualTo(1);
        assertThat(godoApiPort.calls).containsExactly("10000001:y");
    }

    private SyncGodoStatusUseCase useCase(
        FakeGoodsRepository goodsRepository,
        FakeGodoApiPort godoApiPort,
        FakeGoodsStatusLogRepository logRepository
    ) {
        return useCase(goodsRepository, godoApiPort, logRepository, new FakeSfoodAdminGoodsPort());
    }

    private SyncGodoStatusUseCase useCase(
        FakeGoodsRepository goodsRepository,
        FakeGodoApiPort godoApiPort,
        FakeGoodsStatusLogRepository logRepository,
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort
    ) {
        return new SyncGodoStatusUseCase(
            goodsRepository,
            godoApiPort,
            logRepository,
            null,
            new GoodsSoldOutPolicy(),
            org.mockito.Mockito.mock(SlackNotifier.class),
            sfoodAdminGoodsPort
        );
    }

    private Goods goods(String godoCode, String erpCode, String onlyMallYn, String soldOutYn) {
        Goods source = GoodsFixtures.normalGoods();
        return new Goods(
            Integer.parseInt(godoCode.substring(godoCode.length() - 1)),
            godoCode,
            erpCode,
            source.getGoodsName(),
            onlyMallYn,
            soldOutYn,
            "Y".equals(onlyMallYn) ? 1 : 20,
            "Y".equals(onlyMallYn) ? 50 : 40,
            150,
            "Y",
            "Y",
            null,
            null,
            source.getStatus(),
            source.getCreatedId(),
            source.getCreatedAt(),
            null,
            null
        );
    }

    private WmsStockItem item(String itemCd, int qty, int validPercent, String lotNo) {
        return new WmsStockItem(itemCd, "Sample", qty, validPercent, lotNo, "OA110", "104", "", "SLP001", "2026-04-01T00:00:00");
    }

    private Map<String, AggregatedStock> soldOutStocks(AggregatedStock... stocks) {
        return mapOf(stocks);
    }

    private Map<String, AggregatedStock> onSaleStocks(AggregatedStock... stocks) {
        return mapOf(stocks);
    }

    private AggregatedStock stock(String itemCd, int qty, Integer validPercent) {
        return new AggregatedStock(itemCd, qty, validPercent, true, true, true);
    }

    private Map<String, AggregatedStock> mapOf(AggregatedStock... stocks) {
        Map<String, AggregatedStock> map = new HashMap<>();
        for (AggregatedStock stock : stocks) {
            map.put(stock.itemCd(), stock);
        }
        return map;
    }

    private Goods errorGoods(String godoCode, String erpCode, String onlyMallYn, String soldOutYn) {
        Goods goods = goods(godoCode, erpCode, onlyMallYn, soldOutYn);
        return new Goods(
            goods.getId(),
            goods.getGodoGoodsCode(),
            goods.getErpGoodsCode(),
            goods.getGoodsName(),
            goods.getOnlyMallYn(),
            goods.getSoldOutYn(),
            goods.getAvailableQty(),
            goods.getSafetyRate(),
            goods.getRemainValidDays(),
            goods.getGodoSyncYn(),
            goods.getErpSyncYn(),
            goods.getDescription(),
            goods.getMessage(),
            GoodsStatus.ERROR,
            goods.getCreatedId(),
            goods.getCreatedAt(),
            goods.getUpdatedId(),
            goods.getUpdatedAt()
        );
    }

    private Goods blockedGoods(String godoCode, String erpCode, String onlyMallYn, String soldOutYn) {
        Goods goods = goods(godoCode, erpCode, onlyMallYn, soldOutYn);
        goods.enableGodoSyncBlock("user@s-food.com");
        return goods;
    }

    private static final class FakeGoodsRepository implements GoodsRepository {
        private final List<Goods> goodsList;
        private final List<Goods> savedGoods = new ArrayList<>();

        private FakeGoodsRepository(Goods... goods) {
            this.goodsList = new ArrayList<>(List.of(goods));
        }

        @Override
        public Goods save(Goods goods) {
            savedGoods.add(goods);
            return goods;
        }

        @Override
        public Optional<Goods> findById(Integer id) {
            return goodsList.stream().filter(goods -> goods.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Goods> findByGodoCode(String godoGoodsCode) {
            return goodsList.stream().filter(goods -> goods.getGodoGoodsCode().equals(godoGoodsCode)).findFirst();
        }

        @Override
        public Optional<Goods> findByErpCode(String erpGoodsCode) {
            return goodsList.stream().filter(goods -> goods.getErpGoodsCode().equals(erpGoodsCode)).findFirst();
        }

        @Override
        public List<Goods> findAll(String soldOutYn, String onlyMallYn, String status, String keyword) {
            return goodsList.stream()
                .filter(goods -> soldOutYn == null || soldOutYn.equals(goods.getSoldOutYn()))
                .filter(goods -> onlyMallYn == null || onlyMallYn.equals(goods.getOnlyMallYn()))
                .filter(goods -> status == null || status.equals(goods.getStatus().name()))
                .toList();
        }

        @Override
        public List<Goods> findAllByOnlyMallYn(String onlyMallYn) {
            return goodsList.stream()
                .filter(goods -> onlyMallYn.equals(goods.getOnlyMallYn()))
                .toList();
        }

        @Override
        public List<Goods> findAllErpSyncNGoods() {
            return List.of();
        }

        @Override
        public void delete(Goods goods) {
        }
    }

    private static final class FakeGoodsStatusLogRepository implements GoodsStatusLogRepository {
        private final List<GoodsStatusLog> savedLogs = new ArrayList<>();

        @Override
        public GoodsStatusLog save(GoodsStatusLog goodsStatusLog) {
            savedLogs.add(goodsStatusLog);
            return goodsStatusLog;
        }

        @Override
        public List<GoodsStatusLog> findAll(String godoGoodsCode, String changedFrom, String changedTo) {
            return List.of();
        }
    }

    private static final class FakeGodoApiPort implements GodoApiPort {
        private final List<String> calls = new ArrayList<>();
        private final Map<String, RuntimeException> failures = new HashMap<>();

        @Override
        public com.sfood.automation.goods.domain.model.GodoGoodsInfo searchGoods(String godoGoodsCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public GodoApiResult updateSoldOutStatus(String godoGoodsCode, String soldOutFl) {
            calls.add(godoGoodsCode + ":" + soldOutFl);
            RuntimeException failure = failures.get(godoGoodsCode);
            if (failure != null) {
                throw failure;
            }
            return new GodoApiResult(true, "000", "success");
        }

        private void failForCode(String godoGoodsCode) {
            failures.put(godoGoodsCode, new GodoApiException());
        }
    }

    private static final class FakeSfoodAdminGoodsPort implements SfoodAdminGoodsPort {
        final List<String> calls = new ArrayList<>();
        final Map<String, RuntimeException> failures = new HashMap<>();

        @Override
        public GodoApiResult restoreToOnSale(String godoGoodsCode) {
            calls.add(godoGoodsCode);
            RuntimeException failure = failures.get(godoGoodsCode);
            if (failure != null) {
                throw failure;
            }
            return new GodoApiResult(true, "SUCCESS", "정상 전환 완료");
        }

        void failForCode(String godoGoodsCode) {
            failures.put(godoGoodsCode, new SfoodAdminException("admin error"));
        }
    }
}
