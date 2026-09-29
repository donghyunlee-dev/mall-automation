package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.common.exception.GodoApiException;
import com.sfood.automation.common.exception.SfoodAdminException;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusItemResponse;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusRequest;
import com.sfood.automation.goods.application.dto.ForceSyncGodoStatusResponse;
import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.GodoApiResult;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.model.GoodsFixtures;
import com.sfood.automation.goods.domain.model.GoodsStatusLog;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import com.sfood.automation.goods.domain.port.GoodsStatusLogRepository;
import com.sfood.automation.goods.domain.port.SfoodAdminGoodsPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ForceSyncGodoStatusUseCaseTest {

    @Test
    void should_force_sync_eligible_goods_and_update_local_state() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(goodsRepository, godoApiPort, logRepository, new FakeSfoodAdminGoodsPort());

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("10000001"), "Y"),
            "user@s-food.com"
        );

        assertThat(godoApiPort.calls).containsExactly("10000001:y");
        assertThat(goodsRepository.savedGoods).hasSize(1);
        assertThat(goodsRepository.savedGoods.get(0).getSoldOutYn()).isEqualTo("Y");
        assertThat(goodsRepository.savedGoods.get(0).getUpdatedId()).isEqualTo("user@s-food.com");
        assertThat(logRepository.savedLogs).hasSize(1);
        assertThat(logRepository.savedLogs.get(0).beforeStatus()).isEqualTo("ON_SALE");
        assertThat(logRepository.savedLogs.get(0).afterStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(logRepository.savedLogs.get(0).validPercent()).isEqualTo(0);
        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("10000001", "SUCCESS", null, "Y")
        );
    }

    @Test
    void should_skip_blocked_goods_without_calling_godo_api() {
        Goods goods = goods("10000001", "ERP-0001", "N");
        goods.enableGodoSyncBlock("user@s-food.com");
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods);
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
            goodsRepository,
            godoApiPort,
            new FakeGoodsStatusLogRepository(),
            new FakeSfoodAdminGoodsPort()
        );

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("10000001"), "Y"),
            "user@s-food.com"
        );

        assertThat(godoApiPort.calls).isEmpty();
        assertThat(goodsRepository.savedGoods).isEmpty();
        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("10000001", "SKIPPED", "Godo sync is blocked", "N")
        );
    }

    @Test
    void should_force_sync_non_normal_goods_when_other_eligibility_conditions_match() {
        Goods goods = goods("10000001", "ERP-0001", "N");
        goods.markErpError("ERP mismatch");
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods);
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
            goodsRepository,
            godoApiPort,
            logRepository,
            new FakeSfoodAdminGoodsPort()
        );

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("10000001"), "Y"),
            "user@s-food.com"
        );

        assertThat(godoApiPort.calls).containsExactly("10000001:y");
        assertThat(goodsRepository.savedGoods).hasSize(1);
        assertThat(goodsRepository.savedGoods.get(0).getSoldOutYn()).isEqualTo("Y");
        assertThat(goodsRepository.savedGoods.get(0).getUpdatedId()).isEqualTo("user@s-food.com");
        assertThat(logRepository.savedLogs).hasSize(1);
        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("10000001", "SUCCESS", null, "Y")
        );
    }

    @Test
    void should_return_not_found_for_unknown_godo_goods_code() {
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
            new FakeGoodsRepository(),
            new FakeGodoApiPort(),
            new FakeGoodsStatusLogRepository(),
            new FakeSfoodAdminGoodsPort()
        );

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("99999999"), "Y"),
            "user@s-food.com"
        );

        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("99999999", "NOT_FOUND", "Goods not found", null)
        );
    }

    @Test
    void should_not_update_goods_or_logs_when_godo_api_fails() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "N"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        godoApiPort.failForCode("10000001");
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(goodsRepository, godoApiPort, logRepository, new FakeSfoodAdminGoodsPort());

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("10000001"), "Y"),
            "user@s-food.com"
        );

        assertThat(goodsRepository.savedGoods).isEmpty();
        assertThat(logRepository.savedLogs).isEmpty();
        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("10000001", "FAILED", "API error", "N")
        );
    }

    @Test
    void should_use_sfood_admin_when_restoring_to_on_sale() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "Y"));
        FakeGodoApiPort godoApiPort = new FakeGodoApiPort();
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
        FakeGoodsStatusLogRepository logRepository = new FakeGoodsStatusLogRepository();
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
            goodsRepository, godoApiPort, logRepository, sfoodAdminGoodsPort
        );

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("10000001"), "N"),
            "user@s-food.com"
        );

        assertThat(godoApiPort.calls).isEmpty();
        assertThat(sfoodAdminGoodsPort.calls).containsExactly("10000001");
        assertThat(goodsRepository.savedGoods).hasSize(1);
        assertThat(goodsRepository.savedGoods.get(0).getSoldOutYn()).isEqualTo("N");
        assertThat(logRepository.savedLogs.get(0).beforeStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(logRepository.savedLogs.get(0).afterStatus()).isEqualTo("ON_SALE");
        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("10000001", "SUCCESS", null, "N")
        );
    }

    @Test
    void should_return_failed_when_sfood_admin_throws_on_restore() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository(goods("10000001", "ERP-0001", "Y"));
        FakeSfoodAdminGoodsPort sfoodAdminGoodsPort = new FakeSfoodAdminGoodsPort();
        sfoodAdminGoodsPort.failForCode("10000001");
        ForceSyncGodoStatusUseCase useCase = new ForceSyncGodoStatusUseCase(
            goodsRepository, new FakeGodoApiPort(), new FakeGoodsStatusLogRepository(), sfoodAdminGoodsPort
        );

        ForceSyncGodoStatusResponse response = useCase.execute(
            new ForceSyncGodoStatusRequest(List.of("10000001"), "N"),
            "user@s-food.com"
        );

        assertThat(goodsRepository.savedGoods).isEmpty();
        assertThat(response.items()).containsExactly(
            new ForceSyncGodoStatusItemResponse("10000001", "FAILED", "API error", "Y")
        );
    }

    private Goods goods(String godoCode, String erpCode, String soldOutYn) {
        Goods source = GoodsFixtures.godoSyncedGoods();
        return new Goods(
            Integer.parseInt(godoCode.substring(godoCode.length() - 1)),
            godoCode,
            erpCode,
            source.getGoodsName(),
            source.getOnlyMallYn(),
            soldOutYn,
            source.getAvailableQty(),
            source.getSafetyRate(),
            source.getRemainValidDays(),
            "Y",
            "Y",
            "N",
            null,
            null,
            GoodsStatus.NORMAL,
            source.getCreatedId(),
            source.getCreatedAt(),
            null,
            null
        );
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
            return List.of();
        }

        @Override
        public List<Goods> findAllByOnlyMallYn(String onlyMallYn) {
            return List.of();
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
