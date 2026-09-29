package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteGoodsUseCaseTest {

    @Test
    void should_hide_when_godo_sync_is_y() {
        FakeRepository repository = new FakeRepository(com.sfood.automation.goods.domain.model.GoodsFixtures.godoSyncedGoods());
        DeleteGoodsUseCase useCase = new DeleteGoodsUseCase(repository);
        useCase.execute(1);
        assertThat(repository.deleted).isFalse();
        assertThat(repository.saved.getStatus().name()).isEqualTo("HIDE");
    }

    @Test
    void should_delete_physically_when_godo_sync_is_n() {
        FakeRepository repository = new FakeRepository(com.sfood.automation.goods.domain.model.GoodsFixtures.normalGoods());
        DeleteGoodsUseCase useCase = new DeleteGoodsUseCase(repository);
        useCase.execute(1);
        assertThat(repository.deleted).isTrue();
    }

    private static class FakeRepository implements GoodsRepository {
        private final Goods goods;
        private Goods saved;
        private boolean deleted;
        private FakeRepository(Goods goods) { this.goods = goods; }
        @Override public Goods save(Goods goods) { this.saved = goods; return goods; }
        @Override public Optional<Goods> findById(Integer id) { return Optional.of(goods); }
        @Override public Optional<Goods> findByGodoCode(String godoGoodsCode) { return Optional.empty(); }
        @Override public Optional<Goods> findByErpCode(String erpGoodsCode) { return Optional.empty(); }
        @Override public List<Goods> findAll(String soldOutYn, String onlyMallYn, String status, String keyword) { return List.of(); }
        @Override public List<Goods> findAllByOnlyMallYn(String onlyMallYn) { return List.of(); }
        @Override public List<Goods> findAllErpSyncNGoods() { return List.of(); }
        @Override public void delete(Goods goods) { this.deleted = true; }
    }
}
