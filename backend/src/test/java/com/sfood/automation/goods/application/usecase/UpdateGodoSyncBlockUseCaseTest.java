package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.common.exception.GoodsSyncBlockValidationException;
import com.sfood.automation.goods.application.dto.UpdateGodoSyncBlockRequest;
import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateGodoSyncBlockUseCaseTest {

    @Test
    void should_enable_block_when_goods_is_normal() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        Goods goods = normalGoods(1, GoodsStatus.NORMAL);
        goodsRepository.byId.put(1, goods);
        UpdateGodoSyncBlockUseCase useCase = new UpdateGodoSyncBlockUseCase(goodsRepository);

        var response = useCase.execute(1, new UpdateGodoSyncBlockRequest("Y"), "user@s-food.com");

        assertThat(response.godoSyncBlockedYn()).isEqualTo("Y");
        assertThat(goodsRepository.saved.getGodoSyncBlockedYn()).isEqualTo("Y");
        assertThat(goodsRepository.saved.getUpdatedId()).isEqualTo("user@s-food.com");
    }

    @Test
    void should_reject_enable_when_goods_is_not_normal() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        goodsRepository.byId.put(1, normalGoods(1, GoodsStatus.ERROR));
        UpdateGodoSyncBlockUseCase useCase = new UpdateGodoSyncBlockUseCase(goodsRepository);

        assertThatThrownBy(() -> useCase.execute(1, new UpdateGodoSyncBlockRequest("Y"), "user@s-food.com"))
            .isInstanceOf(GoodsSyncBlockValidationException.class)
            .hasMessage("Godo sync block can only be enabled for NORMAL goods");
    }

    @Test
    void should_disable_block_even_when_goods_is_not_normal() {
        FakeGoodsRepository goodsRepository = new FakeGoodsRepository();
        Goods goods = normalGoods(1, GoodsStatus.ERROR);
        goods.enableGodoSyncBlock("user@s-food.com");
        goodsRepository.byId.put(1, goods);
        UpdateGodoSyncBlockUseCase useCase = new UpdateGodoSyncBlockUseCase(goodsRepository);

        var response = useCase.execute(1, new UpdateGodoSyncBlockRequest("N"), "user@s-food.com");

        assertThat(response.godoSyncBlockedYn()).isEqualTo("N");
        assertThat(goodsRepository.saved.getGodoSyncBlockedYn()).isEqualTo("N");
    }

    private Goods normalGoods(Integer id, GoodsStatus status) {
        Goods goods = Goods.create("10000001", "ERP-0001", "Goods", "N", "creator@s-food.com").withId(id);
        if (status == GoodsStatus.NORMAL) {
            goods.recoverErpSync();
            return goods;
        }
        if (status == GoodsStatus.ERROR) {
            goods.markErpError("ERP sync error");
            return goods;
        }
        if (status == GoodsStatus.HIDE) {
            goods.hide();
            return goods;
        }
        return goods;
    }

    private static class FakeGoodsRepository implements GoodsRepository {
        private final Map<Integer, Goods> byId = new HashMap<>();
        private Goods saved;

        @Override
        public Goods save(Goods goods) {
            saved = goods;
            byId.put(goods.getId(), goods);
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
            return Optional.empty();
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
