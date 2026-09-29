package com.sfood.automation.dashboard.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardPeriodResolverTest {

    private final DashboardPeriodResolver resolver = new DashboardPeriodResolver();

    @Test
    void should_resolve_daily_as_the_given_date_itself() {
        DashboardPeriodResolver.DateRange range = resolver.resolve(
            DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null
        );

        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 3, 24));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 3, 24));
    }

    @Test
    void should_resolve_weekly_as_monday_of_that_week_through_the_given_date() {
        // 2026-03-24는 화요일
        DashboardPeriodResolver.DateRange range = resolver.resolve(
            DashboardPeriod.WEEKLY, LocalDate.of(2026, 3, 24), null, null, null
        );

        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 3, 23)); // 월요일
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 3, 24));
    }

    @Test
    void should_resolve_monthly_as_first_to_last_day_of_month() {
        DashboardPeriodResolver.DateRange range = resolver.resolve(
            DashboardPeriod.MONTHLY, null, YearMonth.of(2026, 3), null, null
        );

        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void should_resolve_quarterly_as_first_to_last_day_of_quarter() {
        DashboardPeriodResolver.DateRange range = resolver.resolve(
            DashboardPeriod.QUARTERLY, null, null, 2026, 1
        );

        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void should_resolve_fourth_quarter_correctly() {
        DashboardPeriodResolver.DateRange range = resolver.resolve(
            DashboardPeriod.QUARTERLY, null, null, 2026, 4
        );

        assertThat(range.start()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(range.end()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void should_throw_when_daily_period_missing_date() {
        assertThatThrownBy(() -> resolver.resolve(DashboardPeriod.DAILY, null, null, null, null))
            .isInstanceOf(DashboardQueryException.class);
    }

    @Test
    void should_throw_when_monthly_period_missing_year_month() {
        assertThatThrownBy(() -> resolver.resolve(DashboardPeriod.MONTHLY, null, null, null, null))
            .isInstanceOf(DashboardQueryException.class);
    }

    @Test
    void should_throw_when_quarterly_period_missing_year_or_quarter() {
        assertThatThrownBy(() -> resolver.resolve(DashboardPeriod.QUARTERLY, null, null, 2026, null))
            .isInstanceOf(DashboardQueryException.class);
    }

    @Test
    void should_resolve_day_buckets_ending_at_given_date() {
        List<DashboardPeriodResolver.DateRange> buckets = resolver.resolveTrendBuckets(
            DashboardPeriodResolver.TrendBucketUnit.DAY, 3, LocalDate.of(2026, 3, 24)
        );

        assertThat(buckets).hasSize(3);
        assertThat(buckets.get(0).start()).isEqualTo(LocalDate.of(2026, 3, 22));
        assertThat(buckets.get(2).start()).isEqualTo(LocalDate.of(2026, 3, 24));
        assertThat(buckets.get(2).end()).isEqualTo(LocalDate.of(2026, 3, 24));
    }

    @Test
    void should_resolve_week_buckets_as_monday_to_sunday_ending_at_given_date_week() {
        // 2026-03-24는 화요일 -> 해당 주 월요일은 2026-03-23
        List<DashboardPeriodResolver.DateRange> buckets = resolver.resolveTrendBuckets(
            DashboardPeriodResolver.TrendBucketUnit.WEEK, 2, LocalDate.of(2026, 3, 24)
        );

        assertThat(buckets).hasSize(2);
        assertThat(buckets.get(1).start()).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(buckets.get(1).end()).isEqualTo(LocalDate.of(2026, 3, 29));
        assertThat(buckets.get(0).start()).isEqualTo(LocalDate.of(2026, 3, 16));
    }

    @Test
    void should_resolve_month_buckets_ending_at_given_date_month() {
        List<DashboardPeriodResolver.DateRange> buckets = resolver.resolveTrendBuckets(
            DashboardPeriodResolver.TrendBucketUnit.MONTH, 3, LocalDate.of(2026, 3, 24)
        );

        assertThat(buckets).hasSize(3);
        assertThat(buckets.get(0).start()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(buckets.get(2).start()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(buckets.get(2).end()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void should_resolve_quarter_buckets_crossing_year_boundary() {
        List<DashboardPeriodResolver.DateRange> buckets = resolver.resolveTrendBuckets(
            DashboardPeriodResolver.TrendBucketUnit.QUARTER, 3, LocalDate.of(2026, 3, 24)
        );

        assertThat(buckets).hasSize(3);
        assertThat(buckets.get(0).start()).isEqualTo(LocalDate.of(2025, 7, 1)); // 2025 Q3
        assertThat(buckets.get(1).start()).isEqualTo(LocalDate.of(2025, 10, 1)); // 2025 Q4
        assertThat(buckets.get(2).start()).isEqualTo(LocalDate.of(2026, 1, 1)); // 2026 Q1
        assertThat(buckets.get(2).end()).isEqualTo(LocalDate.of(2026, 3, 31));
    }
}
