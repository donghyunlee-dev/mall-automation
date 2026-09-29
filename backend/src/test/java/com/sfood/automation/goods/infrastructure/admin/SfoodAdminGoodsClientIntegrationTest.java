package com.sfood.automation.goods.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sfood.automation.goods.domain.model.GodoApiResult;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "app.batch.scheduler.auto-start=false"
})
class SfoodAdminGoodsClientIntegrationTest {

    private static final Path DATABASE_PATH = Path.of("build/tmp/live-sfood-admin-integration.db");

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) throws Exception {
        Files.deleteIfExists(DATABASE_PATH);
        registry.add("app.database.path", () -> DATABASE_PATH.toString());
    }

    @Autowired
    private SfoodAdminGoodsClient goodsClient;

    @Autowired
    private SfoodAdminProperties properties;

    @Test
    void should_restore_test_goods_to_on_sale_via_real_service() {
        assumeTrue(isLiveSfoodAdminEnabled(),
            "Set LIVE_SFOOD_ADMIN=true or -DliveSfoodAdmin=true to run live integration test");
        assumeFalse(properties.getTestGoodsCode() == null || properties.getTestGoodsCode().isBlank(),
            "Set SFOOD_ADMIN_TEST_GOODS_CODE env var to a valid goods code");

        GodoApiResult result = goodsClient.restoreToOnSale(properties.getTestGoodsCode());

        System.out.println("LIVE_SFOOD_ADMIN_SUMMARY");
        System.out.println("testGoodsCode=" + properties.getTestGoodsCode());
        System.out.println("result.success=" + result.success());
        System.out.println("result.message=" + result.resultMessage());

        assertThat(result.success()).isTrue();
    }

    private boolean isLiveSfoodAdminEnabled() {
        return Boolean.getBoolean("liveSfoodAdmin") || Boolean.parseBoolean(System.getenv("LIVE_SFOOD_ADMIN"));
    }
}
