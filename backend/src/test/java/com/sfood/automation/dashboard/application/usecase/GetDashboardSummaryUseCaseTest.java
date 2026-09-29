package com.sfood.automation.dashboard.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.application.dto.DashboardSummaryResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class GetDashboardSummaryUseCaseTest {

    @Test
    void should_use_order_date_summary_for_daily_period() {
        FakeDashboardQueryRepository repository = new FakeDashboardQueryRepository();
        GetDashboardSummaryUseCase useCase = new GetDashboardSummaryUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null);

        assertThat(repository.orderDateCalled).isTrue();
        assertThat(repository.shippedDateCalled).isFalse();
        assertThat(repository.lastStart).isEqualTo(LocalDate.of(2026, 3, 24));
        assertThat(repository.lastEnd).isEqualTo(LocalDate.of(2026, 3, 24));
    }

    @Test
    void should_use_shipped_date_summary_for_quarterly_period() {
        FakeDashboardQueryRepository repository = new FakeDashboardQueryRepository();
        GetDashboardSummaryUseCase useCase = new GetDashboardSummaryUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriod.QUARTERLY, null, null, 2026, 1);

        assertThat(repository.shippedDateCalled).isTrue();
        assertThat(repository.orderDateCalled).isFalse();
        assertThat(repository.lastStart).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(repository.lastEnd).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void should_map_summary_to_response() {
        FakeDashboardQueryRepository repository = new FakeDashboardQueryRepository();
        repository.result = new DashboardSummary(new BigDecimal("10000"), 2, 2, 1.5, 3.0);
        GetDashboardSummaryUseCase useCase = new GetDashboardSummaryUseCase(repository, new DashboardPeriodResolver());

        DashboardSummaryResponse response = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null);

        assertThat(response.revenue()).isEqualByComparingTo("10000");
        assertThat(response.orderCount()).isEqualTo(2);
        assertThat(response.aov()).isEqualByComparingTo("5000.00");
    }

    private static final class FakeDashboardQueryRepository implements DashboardQueryRepository {
        private boolean orderDateCalled;
        private boolean shippedDateCalled;
        private LocalDate lastStart;
        private LocalDate lastEnd;
        private DashboardSummary result = DashboardSummary.empty();

        @Override
        public DashboardSummary summaryByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
            orderDateCalled = true;
            lastStart = startInclusive;
            lastEnd = endInclusive;
            return result;
        }

        @Override
        public DashboardSummary summaryByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
            shippedDateCalled = true;
            lastStart = startInclusive;
            lastEnd = endInclusive;
            return result;
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.HourlyRevenue> hourlyRevenue(LocalDate date) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.GradeRevenue> gradeRevenueByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.GradeRevenue> gradeRevenueByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.GradeDetail> gradeDetailByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.GradeDetail> gradeDetailByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.TopItem> topItemsByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
            return java.util.List.of();
        }

        @Override
        public java.util.List<com.sfood.automation.dashboard.domain.model.TopItem> topItemsByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
            return java.util.List.of();
        }
    }
}
