package com.sfood.automation.batch.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.domain.model.GodoOrder;
import com.sfood.automation.dashboard.domain.model.GodoOrderItem;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchQuery;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;
import com.sfood.automation.dashboard.domain.port.GodoOrderItemRepository;
import com.sfood.automation.dashboard.domain.port.GodoOrderRepository;
import com.sfood.automation.dashboard.domain.port.GodoOrderSearchPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderSyncBatchJobTest {

    @Test
    void should_search_using_21_day_rolling_window_ending_at_given_date() {
        FakeSearchPort searchPort = new FakeSearchPort(List.of(emptyResult()));
        OrderSyncBatchJob job = new OrderSyncBatchJob(searchPort, new FakeOrderRepository(), new FakeOrderItemRepository());

        job.run(LocalDate.of(2026, 3, 24));

        assertThat(searchPort.queries).hasSize(1);
        GodoOrderSearchQuery query = searchPort.queries.get(0);
        assertThat(query.dateType()).isEqualTo("order");
        assertThat(query.startDate()).isEqualTo("2026-03-04"); // 2026-03-24 - 20일
        assertThat(query.endDate()).isEqualTo("2026-03-24");
        assertThat(query.lastOrder()).isNull();
    }

    @Test
    void should_paginate_using_next_last_order_until_has_more_is_false() {
        GodoOrderSearchResult page1 = new GodoOrderSearchResult(List.of(order(5L)), List.of(), true, 5L);
        GodoOrderSearchResult page2 = new GodoOrderSearchResult(List.of(order(3L)), List.of(), false, 3L);
        FakeSearchPort searchPort = new FakeSearchPort(List.of(page1, page2));
        OrderSyncBatchJob job = new OrderSyncBatchJob(searchPort, new FakeOrderRepository(), new FakeOrderItemRepository());

        job.run(LocalDate.of(2026, 3, 24));

        assertThat(searchPort.queries).hasSize(2);
        assertThat(searchPort.queries.get(0).lastOrder()).isNull();
        assertThat(searchPort.queries.get(1).lastOrder()).isEqualTo("5");
    }

    @Test
    void should_upsert_orders_and_items_from_every_page() {
        GodoOrderSearchResult page1 = new GodoOrderSearchResult(List.of(order(5L)), List.of(item(1L, 5L)), true, 5L);
        GodoOrderSearchResult page2 = new GodoOrderSearchResult(List.of(order(3L)), List.of(item(2L, 3L)), false, 3L);
        FakeSearchPort searchPort = new FakeSearchPort(List.of(page1, page2));
        FakeOrderRepository orderRepository = new FakeOrderRepository();
        FakeOrderItemRepository itemRepository = new FakeOrderItemRepository();
        OrderSyncBatchJob job = new OrderSyncBatchJob(searchPort, orderRepository, itemRepository);

        job.run(LocalDate.of(2026, 3, 24));

        assertThat(orderRepository.upserted).extracting(GodoOrder::orderNo).containsExactly(5L, 3L);
        assertThat(itemRepository.upserted).extracting(GodoOrderItem::orderNo).containsExactly(5L, 3L);
    }

    @Test
    void should_return_summary_message_with_total_counts_across_pages() {
        GodoOrderSearchResult page1 = new GodoOrderSearchResult(List.of(order(5L), order(4L)), List.of(item(1L, 5L)), true, 4L);
        GodoOrderSearchResult page2 = new GodoOrderSearchResult(List.of(order(3L)), List.of(), false, 3L);
        FakeSearchPort searchPort = new FakeSearchPort(List.of(page1, page2));
        OrderSyncBatchJob job = new OrderSyncBatchJob(searchPort, new FakeOrderRepository(), new FakeOrderItemRepository());

        String summary = job.run(LocalDate.of(2026, 3, 24));

        assertThat(summary).contains("3").contains("1");
    }

    private GodoOrderSearchResult emptyResult() {
        return new GodoOrderSearchResult(List.of(), List.of(), false, null);
    }

    private GodoOrder order(long orderNo) {
        return new GodoOrder(
            null, orderNo, "member01", "일반회원", "s1", "2026-03-24 17:52:56",
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
            "now", "now"
        );
    }

    private GodoOrderItem item(long godoLineNo, long orderNo) {
        return new GodoOrderItem(
            null, "GOODS", godoLineNo, orderNo, "상품", 1,
            BigDecimal.TEN, BigDecimal.TEN, "2026-03-25 09:38:10", "2026-03-26",
            "now", "now"
        );
    }

    private static final class FakeSearchPort implements GodoOrderSearchPort {
        private final Deque<GodoOrderSearchResult> pages;
        private final List<GodoOrderSearchQuery> queries = new ArrayList<>();

        private FakeSearchPort(List<GodoOrderSearchResult> pages) {
            this.pages = new ArrayDeque<>(pages);
        }

        @Override
        public GodoOrderSearchResult search(GodoOrderSearchQuery query) {
            queries.add(query);
            return pages.pollFirst();
        }
    }

    private static final class FakeOrderRepository implements GodoOrderRepository {
        private final List<GodoOrder> upserted = new ArrayList<>();

        @Override
        public GodoOrder upsert(GodoOrder order) {
            upserted.add(order);
            return order;
        }

        @Override
        public Optional<GodoOrder> findByOrderNo(long orderNo) {
            return upserted.stream().filter(o -> o.orderNo() == orderNo).findFirst();
        }
    }

    private static final class FakeOrderItemRepository implements GodoOrderItemRepository {
        private final List<GodoOrderItem> upserted = new ArrayList<>();

        @Override
        public GodoOrderItem upsert(GodoOrderItem item) {
            upserted.add(item);
            return item;
        }

        @Override
        public List<GodoOrderItem> findByOrderNo(long orderNo) {
            return upserted.stream().filter(i -> i.orderNo() == orderNo).toList();
        }
    }
}
