package com.sfood.automation.dashboard.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sfood.automation.dashboard.application.dto.GradePivotResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DateBasis;
import com.sfood.automation.dashboard.domain.model.GradeDetail;
import com.sfood.automation.dashboard.domain.model.GradePivotMetric;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetDashboardGradePivotUseCaseTest {

    @Test
    void should_build_matrix_with_zero_for_grade_missing_in_a_bucket() {
        FakeRepository repository = new FakeRepository(List.of(
            List.of(new GradeDetail("VIP", new BigDecimal("10000"), 1, 1, 1, 1)), // 버킷1: VIP만
            List.of(new GradeDetail("VIP", new BigDecimal("20000"), 1, 1, 1, 1), new GradeDetail("일반회원", new BigDecimal("5000"), 1, 1, 1, 1)) // 버킷2: VIP+일반회원
        ));
        GetDashboardGradePivotUseCase useCase = new GetDashboardGradePivotUseCase(repository, new DashboardPeriodResolver());

        GradePivotResponse response = useCase.execute(
            GradePivotMetric.REVENUE, DashboardPeriodResolver.TrendBucketUnit.DAY, 2, LocalDate.of(2026, 3, 24)
        );

        assertThat(response.bucketEnds()).hasSize(2);
        assertThat(response.byGrade().get("VIP")).containsExactly(new BigDecimal("10000"), new BigDecimal("20000"));
        assertThat(response.byGrade().get("일반회원")).containsExactly(BigDecimal.ZERO, new BigDecimal("5000"));
    }

    @Test
    void should_use_shipped_date_for_day_bucket_when_date_basis_is_shipped() {
        FakeRepository repository = new FakeRepository(List.of(
            List.of(new GradeDetail("VIP", new BigDecimal("10000"), 1, 1, 1, 1))
        ));
        GetDashboardGradePivotUseCase useCase = new GetDashboardGradePivotUseCase(repository, new DashboardPeriodResolver());

        useCase.execute(
            GradePivotMetric.REVENUE, DashboardPeriodResolver.TrendBucketUnit.DAY, 1, LocalDate.of(2026, 3, 24), DateBasis.SHIPPED
        );

        assertThat(repository.shippedDateCalls).isEqualTo(1);
        assertThat(repository.orderDateCalls).isEqualTo(0);
    }

    @Test
    void should_extract_avg_sku_metric_when_requested() {
        FakeRepository repository = new FakeRepository(List.of(
            List.of(new GradeDetail("VIP", new BigDecimal("10000"), 1, 1, 2.5, 4.0))
        ));
        GetDashboardGradePivotUseCase useCase = new GetDashboardGradePivotUseCase(repository, new DashboardPeriodResolver());

        GradePivotResponse response = useCase.execute(
            GradePivotMetric.AVG_SKU, DashboardPeriodResolver.TrendBucketUnit.DAY, 1, LocalDate.of(2026, 3, 24)
        );

        assertThat(response.byGrade().get("VIP").get(0)).isEqualByComparingTo("2.5");
    }

    private static final class FakeRepository implements DashboardQueryRepository {
        private final List<List<GradeDetail>> perBucket;
        private int callIndex;
        private int orderDateCalls;
        private int shippedDateCalls;

        private FakeRepository(List<List<GradeDetail>> perBucket) {
            this.perBucket = perBucket;
        }

        private List<GradeDetail> next() {
            return perBucket.get(callIndex++);
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
        public List<GradeDetail> gradeDetailByOrderDate(LocalDate s, LocalDate e) {
            orderDateCalls++;
            return next();
        }

        @Override
        public List<GradeDetail> gradeDetailByShippedDate(LocalDate s, LocalDate e) {
            shippedDateCalls++;
            return next();
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
