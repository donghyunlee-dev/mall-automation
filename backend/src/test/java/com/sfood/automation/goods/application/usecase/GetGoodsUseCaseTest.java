package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.enums.GoodsStatus;
import com.sfood.automation.goods.domain.model.Goods;
import com.sfood.automation.goods.domain.port.GoodsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetGoodsUseCaseTest {

    @Test
    void should_return_goods_detail_with_missing_table_fields() {
        Instant createdAt = Instant.parse("2026-04-01T09:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-02T10:30:00Z");
        Goods goods = new Goods(
            1,
            "10000001",
            "ERP-0001",
            "Sample Goods",
            "N",
            "N",
            20,
            50,
            150,
            "Y",
            "Y",
            "detail note",
            "sync message",
            GoodsStatus.NORMAL,
            "creator@s-food.com",
            createdAt,
            "editor@s-food.com",
            updatedAt
        );
        GetGoodsUseCase useCase = new GetGoodsUseCase(new FakeGoodsRepository(goods));

        var response = useCase.execute(1);

        assertThat(response.description()).isEqualTo("detail note");
        assertThat(response.createdId()).isEqualTo("creator@s-food.com");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedId()).isEqualTo("editor@s-food.com");
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
        assertThat(response.remainValidDays()).isEqualTo(150);
    }

    private static final class FakeGoodsRepository implements GoodsRepository {
        private final Goods goods;

        private FakeGoodsRepository(Goods goods) {
            this.goods = goods;
        }

        @Override
        public Goods save(Goods goods) {
            return goods;
        }

        @Override
        public Optional<Goods> findById(Integer id) {
            return Optional.of(goods);
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
