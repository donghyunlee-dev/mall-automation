package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetGoodsListUseCaseTest {

    @Test
    void should_return_updated_metadata_when_goods_was_modified() {
        Instant createdAt = Instant.parse("2026-04-01T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-02T10:30:00Z");
        GetGoodsListUseCase useCase = new GetGoodsListUseCase(new FakeGoodsRepository(
            new Goods(1, "10000001", "ERP-0001", "Sample Goods", "N", "N", 20, 50, 150,
                "Y", "Y", null, null, GoodsStatus.NORMAL, "creator@s-food.com", createdAt, "editor@s-food.com", updatedAt)
        ));

        var response = useCase.execute("N", null, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).recentUpdatedAt()).isEqualTo(updatedAt);
        assertThat(response.items().get(0).recentUpdatedBy()).isEqualTo("editor@s-food.com");
    }

    @Test
    void should_fallback_to_created_metadata_when_goods_was_never_modified() {
        Instant createdAt = Instant.parse("2026-04-01T09:00:00Z");
        GetGoodsListUseCase useCase = new GetGoodsListUseCase(new FakeGoodsRepository(
            new Goods(1, "10000001", "ERP-0001", "Sample Goods", "N", "N", 20, 50, 150,
                "Y", "Y", null, null, GoodsStatus.NORMAL, "creator@s-food.com", createdAt, null, null)
        ));

        var response = useCase.execute("N", null, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).recentUpdatedAt()).isEqualTo(createdAt);
        assertThat(response.items().get(0).recentUpdatedBy()).isEqualTo("creator@s-food.com");
    }

    private static final class FakeGoodsRepository implements GoodsRepository {
        private final List<Goods> goods;

        private FakeGoodsRepository(Goods goods) {
            this.goods = List.of(goods);
        }

        @Override
        public Goods save(Goods goods) {
            return goods;
        }

        @Override
        public Optional<Goods> findById(Integer id) {
            return goods.stream().filter(item -> item.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Goods> findByGodoCode(String godoGoodsCode) {
            return goods.stream().filter(item -> item.getGodoGoodsCode().equals(godoGoodsCode)).findFirst();
        }

        @Override
        public Optional<Goods> findByErpCode(String erpGoodsCode) {
            return goods.stream().filter(item -> item.getErpGoodsCode().equals(erpGoodsCode)).findFirst();
        }

        @Override
        public List<Goods> findAll(String soldOutYn, String onlyMallYn, String status, String keyword) {
            return goods;
        }

        @Override
        public List<Goods> findAllByOnlyMallYn(String onlyMallYn) {
            return goods.stream()
                .filter(item -> onlyMallYn.equals(item.getOnlyMallYn()))
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
}
