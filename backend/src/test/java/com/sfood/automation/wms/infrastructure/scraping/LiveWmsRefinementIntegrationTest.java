package com.sfood.automation.wms.infrastructure.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpCookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "app.batch.scheduler.auto-start=false"
})
class LiveWmsRefinementIntegrationTest {

    private static final Path DATABASE_PATH = Path.of("build/tmp/live-wms-integration.db");

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) throws Exception {
        Files.deleteIfExists(DATABASE_PATH);
        registry.add("app.database.path", () -> DATABASE_PATH.toString());
    }

    @Autowired
    private WmsLoginClient wmsLoginClient;

    @Autowired
    private WmsStockClient wmsStockClient;

    @Autowired
    private StockDataRefiner stockDataRefiner;

    @Test
    void should_login_fetch_and_refine_live_wms_stock() {
        Assumptions.assumeTrue(isLiveWmsEnabled(), "Set LIVE_WMS=true or -DliveWms=true to run the live WMS integration test.");

        HttpCookie cookie = wmsLoginClient.login();
        List<WmsStockItem> stockItems = wmsStockClient.fetchAll(cookie);
        Set<String> fetchedItemCodes = stockItems.stream()
            .map(WmsStockItem::itemCd)
            .collect(Collectors.toSet());
        Map<String, AggregatedStock> refinedStocks = stockDataRefiner.refine(stockItems, fetchedItemCodes);

        System.out.println("LIVE_WMS_SUMMARY");
        System.out.println("cookieName=" + cookie.getName());
        System.out.println("rawRowCount=" + stockItems.size());
        System.out.println("distinctItemCodeCount=" + fetchedItemCodes.size());
        System.out.println("refinedRowCount=" + refinedStocks.size());
        System.out.println("aggregatedRowReduction=" + (stockItems.size() - refinedStocks.size()));
        refinedStocks.values().stream()
            .sorted(Comparator.comparingInt(AggregatedStock::totalInvnQty).reversed())
            .limit(5)
            .forEach(stock -> System.out.println(
                "sample itemCd=%s totalInvnQty=%d minValidPercent=%s lotExists=%s allowedZone=%s oa110Lot=%s"
                    .formatted(
                        stock.itemCd(),
                        stock.totalInvnQty(),
                        stock.minValidPercent(),
                        stock.lotExists(),
                        stock.allowedZone(),
                        stock.oa110Lot()
                    )
            ));

        assertThat(cookie.getName()).isNotBlank();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(stockItems).isNotNull();
        assertThat(refinedStocks).hasSizeLessThanOrEqualTo(stockItems.size());
    }

    private boolean isLiveWmsEnabled() {
        return Boolean.getBoolean("liveWms") || Boolean.parseBoolean(System.getenv("LIVE_WMS"));
    }
}
