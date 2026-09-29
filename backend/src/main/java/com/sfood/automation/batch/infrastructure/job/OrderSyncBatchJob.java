package com.sfood.automation.batch.infrastructure.job;

import com.sfood.automation.dashboard.domain.model.GodoOrderSearchQuery;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;
import com.sfood.automation.dashboard.domain.port.GodoOrderItemRepository;
import com.sfood.automation.dashboard.domain.port.GodoOrderRepository;
import com.sfood.automation.dashboard.domain.port.GodoOrderSearchPort;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderSyncBatchJob {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncBatchJob.class);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    /** 9.2.3⑥: 최근 21일 롤링 재조회로 상태 변경(입금대기→배송완료 등)을 반영한다. */
    private static final int ROLLING_WINDOW_DAYS = 21;
    private static final int PAGE_SIZE = 500;

    private final GodoOrderSearchPort godoOrderSearchPort;
    private final GodoOrderRepository godoOrderRepository;
    private final GodoOrderItemRepository godoOrderItemRepository;

    public OrderSyncBatchJob(
        GodoOrderSearchPort godoOrderSearchPort,
        GodoOrderRepository godoOrderRepository,
        GodoOrderItemRepository godoOrderItemRepository
    ) {
        this.godoOrderSearchPort = godoOrderSearchPort;
        this.godoOrderRepository = godoOrderRepository;
        this.godoOrderItemRepository = godoOrderItemRepository;
    }

    public String run() {
        return run(LocalDate.now(KOREA_ZONE).minusDays(1));
    }

    public String run(LocalDate targetDate) {
        LocalDate windowStart = targetDate.minusDays(ROLLING_WINDOW_DAYS - 1L);
        log.info("OrderSyncBatchJob started: window={}~{}", windowStart, targetDate);

        int totalOrders = 0;
        int totalItems = 0;
        String cursor = null;
        while (true) {
            GodoOrderSearchResult result = godoOrderSearchPort.search(
                new GodoOrderSearchQuery("order", windowStart.toString(), targetDate.toString(), PAGE_SIZE, cursor)
            );
            result.orders().forEach(godoOrderRepository::upsert);
            result.items().forEach(godoOrderItemRepository::upsert);
            totalOrders += result.orders().size();
            totalItems += result.items().size();

            if (!result.hasMore() || result.nextLastOrder() == null) {
                break;
            }
            cursor = String.valueOf(result.nextLastOrder());
        }

        String summary = "주문 " + totalOrders + "건, 상품라인 " + totalItems + "건 동기화 (" + windowStart + "~" + targetDate + ")";
        log.info("OrderSyncBatchJob completed: {}", summary);
        return summary;
    }
}
