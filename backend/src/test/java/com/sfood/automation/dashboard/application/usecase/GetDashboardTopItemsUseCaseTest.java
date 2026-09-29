package com.sfood.automation.dashboard.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.TopItem;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetDashboardTopItemsUseCaseTest {

    @Test
    void should_exclude_gift_products() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("[에쓰푸드] 선물세트", new BigDecimal("50000"), 1),
            new TopItem("리얼 스모크햄", new BigDecimal("10000"), 1)
        ));
        GetDashboardTopItemsUseCase useCase = new GetDashboardTopItemsUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null, 6);

        assertThat(result).extracting(TopItem::productName).containsExactly("리얼 스모크햄");
    }

    @Test
    void should_rollup_jambong_slice_variants_into_single_row() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("잠봉 슬라이스 500g", new BigDecimal("30000"), 3),
            new TopItem("잠봉 슬라이스 400g", new BigDecimal("20000"), 2),
            new TopItem("리얼 스모크햄", new BigDecimal("10000"), 1)
        ));
        GetDashboardTopItemsUseCase useCase = new GetDashboardTopItemsUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null, 6);

        TopItem rolledUp = result.stream().filter(i -> i.productName().contains("잠봉")).findFirst().orElseThrow();
        assertThat(rolledUp.revenue()).isEqualByComparingTo("50000");
        assertThat(rolledUp.qty()).isEqualTo(5);
        assertThat(result).hasSize(2); // 롤업 1건 + 리얼 스모크햄
    }

    @Test
    void should_not_rollup_limited_edition_jambong_slice() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("잠봉 슬라이스 500g 한정", new BigDecimal("15000"), 1),
            new TopItem("잠봉 슬라이스 400g", new BigDecimal("20000"), 2)
        ));
        GetDashboardTopItemsUseCase useCase = new GetDashboardTopItemsUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null, 6);

        assertThat(result).extracting(TopItem::productName)
            .contains("잠봉 슬라이스 500g 한정")
            .anyMatch(name -> name.contains("잠봉 슬라이스") && name.contains("총합"));
    }

    @Test
    void should_limit_result_size_and_sort_by_revenue_descending() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("A", new BigDecimal("1000"), 1),
            new TopItem("B", new BigDecimal("3000"), 1),
            new TopItem("C", new BigDecimal("2000"), 1)
        ));
        GetDashboardTopItemsUseCase useCase = new GetDashboardTopItemsUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null, 2);

        assertThat(result).extracting(TopItem::productName).containsExactly("B", "C");
    }

    private static final class FakeRepository implements DashboardQueryRepository {
        private final List<TopItem> items;

        private FakeRepository(List<TopItem> items) {
            this.items = items;
        }

        @Override
        public com.sfood.automation.dashboard.domain.model.DashboardSummary summaryByOrderDate(LocalDate s, LocalDate e) {
            return com.sfood.automation.dashboard.domain.model.DashboardSummary.empty();
        }

        @Override
        public com.sfood.automation.dashboard.domain.model.DashboardSummary summaryByShippedDate(LocalDate s, LocalDate e) {
            return com.sfood.automation.dashboard.domain.model.DashboardSummary.empty();
        }

        @Override
        public List<com.sfood.automation.dashboard.domain.model.HourlyRevenue> hourlyRevenue(LocalDate date) {
            return List.of();
        }

        @Override
        public List<com.sfood.automation.dashboard.domain.model.GradeRevenue> gradeRevenueByOrderDate(LocalDate s, LocalDate e) {
            return List.of();
        }

        @Override
        public List<com.sfood.automation.dashboard.domain.model.GradeRevenue> gradeRevenueByShippedDate(LocalDate s, LocalDate e) {
            return List.of();
        }

        @Override
        public List<com.sfood.automation.dashboard.domain.model.GradeDetail> gradeDetailByOrderDate(LocalDate s, LocalDate e) {
            return List.of();
        }

        @Override
        public List<com.sfood.automation.dashboard.domain.model.GradeDetail> gradeDetailByShippedDate(LocalDate s, LocalDate e) {
            return List.of();
        }

        @Override
        public List<TopItem> topItemsByOrderDate(LocalDate s, LocalDate e) {
            return items;
        }

        @Override
        public List<TopItem> topItemsByShippedDate(LocalDate s, LocalDate e) {
            return items;
        }
    }
}
