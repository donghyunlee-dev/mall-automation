package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import com.sfood.automation.goods.application.dto.UpdateGoodsRequest;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateGoodsUseCaseTest {

    @Test
    void should_update_goods_when_keeping_same_erp_code() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        Goods goods = normalGoods(1, "10000001", "ERP-0001", "Original", "N");
        goodsRepository.byId.put(1, goods);
        goodsRepository.byErpCode.put("ERP-0001", goods);
        UpdateGoodsUseCase useCase = new UpdateGoodsUseCase(goodsRepository);

        var response = useCase.execute(1, new UpdateGoodsRequest("Updated Goods", "ERP-0001", "Y", 7, 40, 90), "user@s-food.com");

        assertThat(response.goodsName()).isEqualTo("Updated Goods");
        assertThat(response.erpGoodsCode()).isEqualTo("ERP-0001");
        assertThat(response.onlyMallYn()).isEqualTo("Y");
        assertThat(response.availableQty()).isEqualTo(7);
        assertThat(response.safetyRate()).isEqualTo(40);
        assertThat(response.remainValidDays()).isEqualTo(90);
        assertThat(goodsRepository.saved.getUpdatedId()).isEqualTo("user@s-food.com");
        assertThat(goodsRepository.saved.getRemainValidDays()).isEqualTo(90);
    }

    @Test
    void should_allow_updating_to_another_goods_erp_code() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        Goods target = normalGoods(1, "10000001", "ERP-0001", "Target", "N");
        Goods other = normalGoods(2, "10000002", "ERP-0002", "Other", "N");
        goodsRepository.byId.put(1, target);
        goodsRepository.byId.put(2, other);
        goodsRepository.byErpCode.put("ERP-0001", target);
        goodsRepository.byErpCode.put("ERP-0002", other);
        UpdateGoodsUseCase useCase = new UpdateGoodsUseCase(goodsRepository);

        var response = useCase.execute(1, new UpdateGoodsRequest("Updated Goods", "ERP-0002", "N", 20, 50, 150), "user@s-food.com");

        assertThat(response.erpGoodsCode()).isEqualTo("ERP-0002");
        assertThat(goodsRepository.saved.getErpGoodsCode()).isEqualTo("ERP-0002");
    }

    private Goods normalGoods(Integer id, String godoGoodsCode, String erpGoodsCode, String goodsName, String onlyMallYn) {
        return Goods.create(godoGoodsCode, erpGoodsCode, goodsName, onlyMallYn, "creator@s-food.com").withId(id);
    }

    private static class FakeGoodsRepository implements GoodsRepository {
        private final Map<Integer, Goods> byId = new HashMap<>();
        private final Map<String, Goods> byErpCode = new HashMap<>();
        private Goods saved;

        @Override
        public Goods save(Goods goods) {
            saved = goods;
            byId.put(goods.getId(), goods);
            byErpCode.put(goods.getErpGoodsCode(), goods);
            return goods;
        }

        @Override
        public Optional<Goods> findById(Integer id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Goods> findByGodoCode(String godoGoodsCode) {
            return Optional.empty();
        }

        @Override
        public Optional<Goods> findByErpCode(String erpGoodsCode) {
            return Optional.ofNullable(byErpCode.get(erpGoodsCode));
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
}
