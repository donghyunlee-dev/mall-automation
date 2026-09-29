package com.sfood.automation.goods.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.support.SharedSqliteTestDatabase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GoodsRepositoryImpl.class, CommonStatusConfigRepositoryImpl.class})
class GoodsRepositoryImplTest {

    @Autowired
    private GoodsJpaRepository goodsJpaRepository;

    @Autowired
    private GoodsRepositoryImpl goodsRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        SharedSqliteTestDatabase.register(registry);
    }

    @BeforeEach
    void setUp() {
        goodsJpaRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        goodsJpaRepository.deleteAll();
    }

    @Test
    void should_exclude_hide_status_from_list_query() {
        saveGoods("1001", "ERP-1", "NORMAL", "N", "Y");
        saveGoods("1002", "ERP-2", "HIDE", "N", "Y");

        var result = goodsRepository.findAll("N", null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGodoGoodsCode()).isEqualTo("1001");
    }

    @Test
    void should_return_only_erp_sync_n_goods() {
        saveGoods("1001", "ERP-1", "NORMAL", "N", "N");
        saveGoods("1002", "ERP-2", "NORMAL", "N", "Y");

        var result = goodsRepository.findAllErpSyncNGoods();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getErpSyncYn()).isEqualTo("N");
    }

    @Test
    void should_exclude_hide_status_from_erp_sync_n_goods() {
        saveGoods("1001", "ERP-1", "HIDE", "N", "N");
        saveGoods("1002", "ERP-2", "NORMAL", "N", "N");

        var result = goodsRepository.findAllErpSyncNGoods();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGodoGoodsCode()).isEqualTo("1002");
    }

    @Test
    void should_filter_by_sold_out_and_only_mall() {
        saveGoods("1001", "ERP-1", "NORMAL", "Y", "Y");
        saveGoods("1002", "ERP-2", "NORMAL", "N", "Y");
        saveGoods("1003", "ERP-3", "NORMAL", "Y", "Y");

        var result = goodsRepository.findAll("Y", "Y", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGodoGoodsCode()).isEqualTo("1003");
    }

    @Test
    void should_return_scope_goods_including_hide_status() {
        saveGoods("1001", "ERP-1", "NORMAL", "N", "Y");
        saveGoods("1002", "ERP-2", "HIDE", "N", "Y");
        saveGoods("1003", "ERP-3", "NORMAL", "N", "Y");

        var result = goodsRepository.findAllByOnlyMallYn("N");

        assertThat(result).extracting(com.sfood.automation.goods.domain.model.Goods::getGodoGoodsCode)
            .containsExactlyInAnyOrder("1001", "1002");
    }

    @Test
    void should_persist_and_read_godo_sync_blocked_flag() {
        saveGoods("1001", "ERP-1", "NORMAL", "N", "Y", "Y");

        var result = goodsRepository.findByGodoCode("1001").orElseThrow();

        assertThat(result.getGodoSyncBlockedYn()).isEqualTo("Y");
    }

    @Test
    void should_allow_duplicate_erp_goods_codes() {
        saveGoods("1001", "ERP-DUP", "NORMAL", "N", "Y");
        saveGoods("1002", "ERP-DUP", "NORMAL", "N", "Y");

        var result = goodsRepository.findAll(null, null, null, null);

        assertThat(result).extracting(com.sfood.automation.goods.domain.model.Goods::getGodoGoodsCode)
            .containsExactlyInAnyOrder("1001", "1002");
    }

    private void saveGoods(String godoCode, String erpCode, String status, String soldOutYn, String erpSyncYn) {
        saveGoods(godoCode, erpCode, status, soldOutYn, erpSyncYn, "N");
    }

    private void saveGoods(String godoCode, String erpCode, String status, String soldOutYn, String erpSyncYn, String godoSyncBlockedYn) {
        GoodsEntity entity = new GoodsEntity();
        entity.setGodoGoodsCode(godoCode);
        entity.setErpGoodsCode(erpCode);
        entity.setGoodsName("Goods-" + godoCode);
        entity.setOnlyMallYn(godoCode.equals("1003") ? "Y" : "N");
        entity.setSoldOutYn(soldOutYn);
        entity.setAvailableQty(20);
        entity.setSafetyRate(50);
        entity.setGodoSyncYn("Y");
        entity.setGodoSyncBlockedYn(godoSyncBlockedYn);
        entity.setErpSyncYn(erpSyncYn);
        entity.setStatus(status);
        entity.setCreatedId("user@s-food.com");
        entity.setCreatedAt(java.time.Instant.now().toString());
        goodsJpaRepository.save(entity);
    }
}
