package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.DuplicateGoodsException;
import com.sfood.automation.goods.application.dto.RegisterGoodsRequest;
import com.sfood.automation.goods.application.dto.RegisterGoodsListResponse;
import com.sfood.automation.goods.application.dto.RegisterGoodsResponse;
import com.sfood.automation.goods.domain.model.CommonStatusConfig;
import com.sfood.automation.goods.domain.model.GodoGoodsInfo;
import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import com.sfood.automation.goods.domain.port.GodoApiPort;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegisterGoodsUseCaseTest {

    @Test
    void should_register_new_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(
            goodsRepository,
            new FakeConfigRepository(),
            new FakeGodoApiPort()
        );

        var response = single(useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "N")), "user@s-food.com"));

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.status()).isEqualTo("UNVERIFIED");
        assertThat(goodsRepository.saved.getErpSyncYn()).isEqualTo("N");
        assertThat(goodsRepository.saved.getGodoSyncYn()).isEqualTo("Y");
        assertThat(goodsRepository.saved.getRemainValidDays()).isEqualTo(150);
        assertThat(goodsRepository.saved.getSafetyRate()).isEqualTo(50);
        assertThat(goodsRepository.saved.getAvailableQty()).isEqualTo(20);
    }

    @Test
    void should_register_multiple_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(
            goodsRepository,
            new FakeConfigRepository(),
            new FakeGodoApiPort()
        );

        var response = useCase.execute(
            List.of(
                new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods 1", "N"),
                new RegisterGoodsRequest("10000002", "ERP-0002", "Sample Goods 2", "Y")
            ),
            "user@s-food.com"
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).extracting("godoGoodsCode").containsExactly("10000001", "10000002");
        assertThat(goodsRepository.savedItems).hasSize(2);
    }

    @Test
    void should_throw_when_godo_code_exists_in_normal_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        goodsRepository.byGodoCode.put("10000001", com.sfood.automation.goods.domain.model.GoodsFixtures.normalGoods());
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(goodsRepository, new FakeConfigRepository(), new FakeGodoApiPort());

        assertThatThrownBy(() -> useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "N")), "user@s-food.com"))
            .isInstanceOf(DuplicateGoodsException.class);
    }

    @Test
    void should_restore_hide_goods_when_godo_code_exists_in_hide_status() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        goodsRepository.byGodoCode.put("10000001", com.sfood.automation.goods.domain.model.GoodsFixtures.hiddenGoods());
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(goodsRepository, new FakeConfigRepository(), new FakeGodoApiPort());

        var response = single(useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "N")), "user@s-food.com"));

        assertThat(response.status()).isEqualTo("NORMAL");
    }

    @Test
    void should_refresh_sold_out_status_when_restoring_hidden_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        goodsRepository.byGodoCode.put("10000001", com.sfood.automation.goods.domain.model.GoodsFixtures.hiddenGoods());
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(
            goodsRepository,
            new FakeConfigRepository(),
            new FakeGodoApiPort("Y")
        );

        useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "N")), "user@s-food.com");

        assertThat(goodsRepository.saved.getStatus()).isEqualTo(com.sfood.automation.goods.domain.enums.GoodsStatus.NORMAL);
        assertThat(goodsRepository.saved.getGodoSyncYn()).isEqualTo("Y");
        assertThat(goodsRepository.saved.getSoldOutYn()).isEqualTo("Y");
    }

    @Test
    void should_keep_godo_sync_n_when_search_result_has_no_matching_goods_for_new_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(
            goodsRepository,
            new FakeConfigRepository(),
            new FakeGodoApiPort("", "", "N")
        );

        useCase.execute(List.of(new RegisterGoodsRequest("9900000733", "ERP-0733", "Missing Godo Goods", "N")), "user@s-food.com");

        assertThat(goodsRepository.saved.getGodoSyncYn()).isEqualTo("N");
        assertThat(goodsRepository.saved.getSoldOutYn()).isEqualTo("N");
    }

    @Test
    void should_reset_godo_sync_n_when_restoring_hidden_goods_and_search_result_has_no_matching_goods() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        goodsRepository.byGodoCode.put("10000001", com.sfood.automation.goods.domain.model.GoodsFixtures.hiddenGoods());
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(
            goodsRepository,
            new FakeConfigRepository(),
            new FakeGodoApiPort("", "", "N")
        );

        useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "N")), "user@s-food.com");

        assertThat(goodsRepository.saved.getStatus()).isEqualTo(com.sfood.automation.goods.domain.enums.GoodsStatus.NORMAL);
        assertThat(goodsRepository.saved.getGodoSyncYn()).isEqualTo("N");
        assertThat(goodsRepository.saved.getSoldOutYn()).isEqualTo("N");
    }

    @Test
    void should_allow_duplicate_erp_code_when_godo_code_is_unique() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        goodsRepository.byErpCode.put("ERP-0001", com.sfood.automation.goods.domain.model.GoodsFixtures.normalGoods());
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(goodsRepository, new FakeConfigRepository(), new FakeGodoApiPort());

        var response = single(useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "N")), "user@s-food.com"));

        assertThat(response.godoGoodsCode()).isEqualTo("10000001");
        assertThat(response.erpGoodsCode()).isEqualTo("ERP-0001");
    }

    @Test
    void should_initialize_exclusive_scope_values() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        RegisterGoodsUseCase useCase = new RegisterGoodsUseCase(goodsRepository, new FakeConfigRepository(), new FakeGodoApiPort());

        useCase.execute(List.of(new RegisterGoodsRequest("10000001", "ERP-0001", "Sample Goods", "Y")), "user@s-food.com");

        assertThat(goodsRepository.saved.getAvailableQty()).isEqualTo(1);
        assertThat(goodsRepository.saved.getSafetyRate()).isEqualTo(50);
    }

    private RegisterGoodsResponse single(RegisterGoodsListResponse response) {
        return response.items().getFirst();
    }

    private static class FakeGoodsRepository implements GoodsRepository {
        private final Map<String, Goods> byGodoCode = new HashMap<>();
        private final Map<String, Goods> byErpCode = new HashMap<>();
        private final List<Goods> savedItems = new java.util.ArrayList<>();
        private Goods saved;
        @Override public Goods save(Goods goods) {
            saved = goods.withId(savedItems.size() + 1);
            savedItems.add(saved);
            byGodoCode.put(saved.getGodoGoodsCode(), saved);
            byErpCode.put(saved.getErpGoodsCode(), saved);
            return saved;
        }
        @Override public Optional<Goods> findById(Integer id) { return Optional.empty(); }
        @Override public Optional<Goods> findByGodoCode(String godoGoodsCode) { return Optional.ofNullable(byGodoCode.get(godoGoodsCode)); }
        @Override public Optional<Goods> findByErpCode(String erpGoodsCode) { return Optional.ofNullable(byErpCode.get(erpGoodsCode)); }
        @Override public List<Goods> findAll(String soldOutYn, String onlyMallYn, String status, String keyword) { return List.of(); }
        @Override public List<Goods> findAllByOnlyMallYn(String onlyMallYn) { return List.of(); }
        @Override public List<Goods> findAllErpSyncNGoods() { return List.of(); }
        @Override public void delete(Goods goods) {}
    }

    private static class FakeConfigRepository implements CommonStatusConfigRepository {
        @Override
        public List<CommonStatusConfig> findAll() {
            return List.of(
                new CommonStatusConfig(2, GoodsScope.GENERAL, 20, 21, 50, 150, null, Instant.now()),
                new CommonStatusConfig(1, GoodsScope.EXCLUSIVE, 1, 2, 50, 150, null, Instant.now())
            );
        }

        @Override
        public Optional<CommonStatusConfig> findByScope(GoodsScope scope) {
            if (scope == GoodsScope.EXCLUSIVE) {
                return Optional.of(new CommonStatusConfig(1, GoodsScope.EXCLUSIVE, 1, 2, 50, 150, null, Instant.now()));
            }
            return Optional.of(new CommonStatusConfig(2, GoodsScope.GENERAL, 20, 21, 50, 150, null, Instant.now()));
        }
        @Override public CommonStatusConfig save(CommonStatusConfig config) { return config; }
    }

    private static class FakeGodoApiPort implements GodoApiPort {
        private final String godoGoodsCode;
        private final String goodsName;
        private final String soldOutYn;

        private FakeGodoApiPort() {
            this("10000001", "Sample Goods", "N");
        }

        private FakeGodoApiPort(String soldOutYn) {
            this("10000001", "Sample Goods", soldOutYn);
        }

        private FakeGodoApiPort(String godoGoodsCode, String goodsName, String soldOutYn) {
            this.godoGoodsCode = godoGoodsCode;
            this.goodsName = goodsName;
            this.soldOutYn = soldOutYn;
        }

        @Override public GodoGoodsInfo searchGoods(String godoGoodsCode) { return new GodoGoodsInfo(this.godoGoodsCode, goodsName, soldOutYn); }
        @Override public com.sfood.automation.goods.domain.model.GodoApiResult updateSoldOutStatus(String godoGoodsCode, String soldOutFl) { return new com.sfood.automation.goods.domain.model.GodoApiResult(true, "000", "success"); }
    }
}
