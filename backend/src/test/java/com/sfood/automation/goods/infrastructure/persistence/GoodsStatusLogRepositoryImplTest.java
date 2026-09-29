package com.sfood.automation.goods.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GoodsStatusLogRepositoryImpl.class)
class GoodsStatusLogRepositoryImplTest {

    @Autowired
    private GoodsStatusLogJpaRepository goodsStatusLogJpaRepository;

    @Autowired
    private GoodsStatusLogRepositoryImpl goodsStatusLogRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @AfterEach
    void tearDown() {
        goodsStatusLogJpaRepository.deleteAll();
    }

    @Test
    void should_return_logs_for_specific_godo_goods_code() {
        saveLog("10000001", "ERP-0001", "2026-03-31T09:00:00Z");
        saveLog("10000002", "ERP-0002", "2026-03-31T10:00:00Z");

        var result = goodsStatusLogRepository.findAll("10000001", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).godoGoodsCode()).isEqualTo("10000001");
    }

    @Test
    void should_filter_logs_by_changed_at_range() {
        saveLog("10000001", "ERP-0001", "2026-03-30T23:59:59Z");
        saveLog("10000001", "ERP-0001", "2026-03-31T12:00:00Z");
        saveLog("10000001", "ERP-0001", "2026-04-01T00:00:00Z");

        var result = goodsStatusLogRepository.findAll(
            null,
            "2026-03-31T00:00:00Z",
            "2026-03-31T23:59:59Z"
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).changedAt()).isEqualTo("2026-03-31T12:00:00Z");
    }

    @Test
    void should_return_logs_in_changed_at_desc_order() {
        saveLog("10000001", "ERP-0001", "2026-03-31T09:00:00Z");
        saveLog("10000001", "ERP-0001", "2026-03-31T11:00:00Z");

        List<com.sfood.automation.goods.domain.model.GoodsStatusLog> result =
            goodsStatusLogRepository.findAll("10000001", null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).changedAt()).isEqualTo("2026-03-31T11:00:00Z");
        assertThat(result.get(1).changedAt()).isEqualTo("2026-03-31T09:00:00Z");
    }

    private void saveLog(String godoGoodsCode, String erpGoodsCode, String changedAt) {
        GoodsStatusLogEntity entity = new GoodsStatusLogEntity();
        entity.setGodoGoodsCode(godoGoodsCode);
        entity.setErpGoodsCode(erpGoodsCode);
        entity.setWmsAvailableQty(15);
        entity.setCriteriaQty(20);
        entity.setSafetyRate(50);
        entity.setValidPercent(75);
        entity.setBeforeStatus("ON_SALE");
        entity.setAfterStatus("OUT_OF_STOCK");
        entity.setChangedAt(changedAt);
        entity.setCreatedAt(changedAt);
        goodsStatusLogJpaRepository.save(entity);
    }
}
