package com.sfood.automation.goods.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.goods.application.dto.UpdateCommonStatusConfigRequest;
import com.sfood.automation.goods.domain.enums.GoodsScope;
import com.sfood.automation.goods.domain.model.CommonStatusConfig;
import com.sfood.automation.goods.domain.port.CommonStatusConfigRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateCommonStatusConfigUseCaseTest {

    @Test
    void should_update_selected_scope_from_path_variable() {
        FakeConfigRepository repository = new FakeConfigRepository();
        UpdateCommonStatusConfigUseCase useCase = new UpdateCommonStatusConfigUseCase(repository);

        var response = useCase.execute(
            GoodsScope.GENERAL,
            new UpdateCommonStatusConfigRequest(10, 11, 40, 150),
            "user@s-food.com"
        );

        assertThat(response.scope()).isEqualTo("GENERAL");
        assertThat(response.outOfStockQty()).isEqualTo(10);
        assertThat(response.onSaleQty()).isEqualTo(11);
        assertThat(response.safetyRate()).isEqualTo(40);
        assertThat(repository.savedConfig.scope()).isEqualTo(GoodsScope.GENERAL);
        assertThat(repository.savedConfig.updatedId()).isEqualTo("user@s-food.com");
    }

    @Test
    void should_update_exclusive_scope() {
        FakeConfigRepository repository = new FakeConfigRepository();
        UpdateCommonStatusConfigUseCase useCase = new UpdateCommonStatusConfigUseCase(repository);

        var response = useCase.execute(
            GoodsScope.EXCLUSIVE,
            new UpdateCommonStatusConfigRequest(3, 4, 60, 150),
            "user@s-food.com"
        );

        assertThat(response.scope()).isEqualTo("EXCLUSIVE");
        assertThat(repository.savedConfig.scope()).isEqualTo(GoodsScope.EXCLUSIVE);
    }

    private static final class FakeConfigRepository implements CommonStatusConfigRepository {

        private CommonStatusConfig savedConfig;

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
            this.savedConfig = config;
            return config;
        }
    }
}
