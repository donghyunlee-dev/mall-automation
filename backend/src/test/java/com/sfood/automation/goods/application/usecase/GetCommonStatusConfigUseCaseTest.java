package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.CommonStatusConfig;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCommonStatusConfigUseCaseTest {

    @Test
    void should_return_all_configs_in_repository_order() {
        GetCommonStatusConfigUseCase useCase = new GetCommonStatusConfigUseCase(new FakeConfigRepository());

        var response = useCase.execute();

        assertThat(response).hasSize(2);
        assertThat(response).extracting("scope").containsExactly("GENERAL", "EXCLUSIVE");
        assertThat(response).extracting("outOfStockQty").containsExactly(20, 1);
    }

    private static class FakeConfigRepository implements CommonStatusConfigRepository {
        @Override
        public List<CommonStatusConfig> findAll() {
            return List.of(
                new CommonStatusConfig(1, GoodsScope.GENERAL, 20, 21, 50, 150, null, Instant.now()),
                new CommonStatusConfig(2, GoodsScope.EXCLUSIVE, 1, 2, 50, 150, null, Instant.now())
            );
        }

        @Override
        public Optional<CommonStatusConfig> findByScope(GoodsScope scope) {
            return findAll().stream().filter(config -> config.scope() == scope).findFirst();
        }

        @Override
        public CommonStatusConfig save(CommonStatusConfig config) {
            return config;
        }
    }
}
