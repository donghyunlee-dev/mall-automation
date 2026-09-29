package com.sfood.automation.dashboard.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.application.dto.DashboardTrendPointResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.model.DateBasis;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetDashboardRevenueTrendUseCaseTest {

    @Test
    void should_use_order_date_summary_for_day_bucket() {
        FakeRepository repository = new FakeRepository();
        GetDashboardRevenueTrendUseCase useCase = new GetDashboardRevenueTrendUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriodResolver.TrendBucketUnit.DAY, 3, LocalDate.of(2026, 3, 24));

        assertThat(repository.orderDateCalls).isEqualTo(3);
        assertThat(repository.shippedDateCalls).isEqualTo(0);
    }

    @Test
    void should_use_shipped_date_summary_for_quarter_bucket() {
        FakeRepository repository = new FakeRepository();
        GetDashboardRevenueTrendUseCase useCase = new GetDashboardRevenueTrendUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriodResolver.TrendBucketUnit.QUARTER, 4, LocalDate.of(2026, 3, 24));

        assertThat(repository.shippedDateCalls).isEqualTo(4);
        assertThat(repository.orderDateCalls).isEqualTo(0);
    }

    @Test
    void should_use_shipped_date_summary_for_day_bucket_when_date_basis_is_shipped() {
        FakeRepository repository = new FakeRepository();
        GetDashboardRevenueTrendUseCase useCase = new GetDashboardRevenueTrendUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriodResolver.TrendBucketUnit.DAY, 3, LocalDate.of(2026, 3, 24), DateBasis.SHIPPED);

        assertThat(repository.shippedDateCalls).isEqualTo(3);
        assertThat(repository.orderDateCalls).isEqualTo(0);
    }

    @Test
    void should_use_order_date_summary_for_quarter_bucket_when_date_basis_is_order() {
        FakeRepository repository = new FakeRepository();
        GetDashboardRevenueTrendUseCase useCase = new GetDashboardRevenueTrendUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(DashboardPeriodResolver.TrendBucketUnit.QUARTER, 2, LocalDate.of(2026, 3, 24), DateBasis.ORDER);

        assertThat(repository.orderDateCalls).isEqualTo(2);
        assertThat(repository.shippedDateCalls).isEqualTo(0);
    }

    @Test
    void should_return_one_point_per_bucket_in_chronological_order() {
        FakeRepository repository = new FakeRepository();
        repository.revenues = List.of(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("300"));
        GetDashboardRevenueTrendUseCase useCase = new GetDashboardRevenueTrendUseCase(repository, new DashboardPeriodResolver());

        List<DashboardTrendPointResponse> result = useCase.execute(DashboardPeriodResolver.TrendBucketUnit.DAY, 3, LocalDate.of(2026, 3, 24));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).start()).isEqualTo(LocalDate.of(2026, 3, 22));
        assertThat(result.get(0).revenue()).isEqualByComparingTo("100");
        assertThat(result.get(2).start()).isEqualTo(LocalDate.of(2026, 3, 24));
        assertThat(result.get(2).revenue()).isEqualByComparingTo("300");
    }

    private static final class FakeRepository implements DashboardQueryRepository {
        private int orderDateCalls;
        private int shippedDateCalls;
        private List<BigDecimal> revenues = new ArrayList<>();
        private int callIndex;

        @Override
        public DashboardSummary summaryByOrderDate(LocalDate startInclusive, LocalDate endInclusive) {
            orderDateCalls++;
            return nextSummary();
        }

        @Override
        public DashboardSummary summaryByShippedDate(LocalDate startInclusive, LocalDate endInclusive) {
            shippedDateCalls++;
            return nextSummary();
        }

        private DashboardSummary nextSummary() {
            if (revenues.isEmpty()) {
                return DashboardSummary.empty();
            }
            BigDecimal revenue = revenues.get(callIndex % revenues.size());
            callIndex++;
            return new DashboardSummary(revenue, 1, 1, 1, 1);
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
