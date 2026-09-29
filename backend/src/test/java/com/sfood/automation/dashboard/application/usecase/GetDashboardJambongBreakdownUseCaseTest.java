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

class GetDashboardJambongBreakdownUseCaseTest {

    @Test
    void should_return_individual_jambong_sku_revenue_without_rolling_up() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("[에쓰푸드] 잠봉 슬라이스 500g (냉동)", new BigDecimal("30000"), 3),
            new TopItem("[에쓰푸드] 잠봉 슬라이스 400g (냉동)", new BigDecimal("20000"), 2),
            new TopItem("[에쓰푸드] 잠봉 슬라이스 500g (냉장)", new BigDecimal("10000"), 1),
            new TopItem("리얼 스모크햄", new BigDecimal("50000"), 1)
        ));
        GetDashboardJambongBreakdownUseCase useCase =
            new GetDashboardJambongBreakdownUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null);

        assertThat(result).extracting(TopItem::productName).containsExactly(
            "[에쓰푸드] 잠봉 슬라이스 500g (냉동)",
            "[에쓰푸드] 잠봉 슬라이스 400g (냉동)",
            "[에쓰푸드] 잠봉 슬라이스 500g (냉장)"
        );
    }

    @Test
    void should_exclude_limited_edition_jambong_slice() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("잠봉 슬라이스 500g 한정", new BigDecimal("15000"), 1),
            new TopItem("잠봉 슬라이스 400g", new BigDecimal("20000"), 2)
        ));
        GetDashboardJambongBreakdownUseCase useCase =
            new GetDashboardJambongBreakdownUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null);

        assertThat(result).extracting(TopItem::productName).containsExactly("잠봉 슬라이스 400g");
    }

    @Test
    void should_return_empty_list_when_no_jambong_items_sold() {
        FakeRepository repository = new FakeRepository(List.of(
            new TopItem("리얼 스모크햄", new BigDecimal("50000"), 1)
        ));
        GetDashboardJambongBreakdownUseCase useCase =
            new GetDashboardJambongBreakdownUseCase(repository, new DashboardPeriodResolver());

        List<TopItem> result = useCase.execute(DashboardPeriod.DAILY, LocalDate.of(2026, 3, 24), null, null, null);

        assertThat(result).isEmpty();
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
