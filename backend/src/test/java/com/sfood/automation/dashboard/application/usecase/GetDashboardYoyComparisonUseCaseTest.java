package com.sfood.automation.dashboard.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.dashboard.application.dto.YoyComparisonResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.application.support.DashboardQueryException;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetDashboardYoyComparisonUseCaseTest {

    @Test
    void should_compare_monthly_with_same_month_previous_year() {
        FakeRepository repository = new FakeRepository();
        GetDashboardYoyComparisonUseCase useCase = new GetDashboardYoyComparisonUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriod.MONTHLY, YearMonth.of(2026, 3), null, null);

        assertThat(repository.ranges).containsExactly(
            new DashboardPeriodResolver.DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)),
            new DashboardPeriodResolver.DateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31))
        );
    }

    @Test
    void should_compare_quarterly_with_same_quarter_previous_year() {
        FakeRepository repository = new FakeRepository();
        GetDashboardYoyComparisonUseCase useCase = new GetDashboardYoyComparisonUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriod.QUARTERLY, null, 2026, 1);

        assertThat(repository.ranges).containsExactly(
            new DashboardPeriodResolver.DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
            new DashboardPeriodResolver.DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31))
        );
    }

    @Test
    void should_reject_daily_and_weekly_periods() {
        FakeRepository repository = new FakeRepository();
        GetDashboardYoyComparisonUseCase useCase = new GetDashboardYoyComparisonUseCase(repository, new DashboardPeriodResolver());

        assertThatThrownBy(() -> useCase.execute(DashboardPeriod.DAILY, null, null, null))
            .isInstanceOf(DashboardQueryException.class);
    }

    @Test
    void should_map_current_and_previous_summaries_to_response() {
        FakeRepository repository = new FakeRepository();
        repository.summaries = List.of(
            new DashboardSummary(new BigDecimal("20000"), 2, 2, 1, 1),
            new DashboardSummary(new BigDecimal("10000"), 1, 1, 1, 1)
        );
        GetDashboardYoyComparisonUseCase useCase = new GetDashboardYoyComparisonUseCase(repository, new DashboardPeriodResolver());

        YoyComparisonResponse response = useCase.execute(DashboardPeriod.MONTHLY, YearMonth.of(2026, 3), null, null);

        assertThat(response.current().revenue()).isEqualByComparingTo("20000");
        assertThat(response.previous().revenue()).isEqualByComparingTo("10000");
    }

    private static final class FakeRepository implements DashboardQueryRepository {
        private final List<DashboardPeriodResolver.DateRange> ranges = new java.util.ArrayList<>();
        private List<DashboardSummary> summaries = List.of();
        private int callIndex;

        @Override
        public DashboardSummary summaryByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
            ranges.add(new DashboardPeriodResolver.DateRange(startInclusive, endInclusive));
            return nextSummary();
        }

        @Override
        public DashboardSummary summaryByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
            ranges.add(new DashboardPeriodResolver.DateRange(startInclusive, endInclusive));
            return nextSummary();
        }

        private DashboardSummary nextSummary() {
            if (summaries.isEmpty()) {
                return DashboardSummary.empty();
            }
            return summaries.get(callIndex++);
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
        public List<com.sfood.automation.dashboard.domain.model.TopItem> topItemsByOrderDate(LocalDate s, LocalDate e) {
            return List.of();
        }

        @Override
        public List<com.sfood.automation.dashboard.domain.model.TopItem> topItemsByShippedDate(LocalDate s, LocalDate e) {
            return List.of();
        }
    }
}
