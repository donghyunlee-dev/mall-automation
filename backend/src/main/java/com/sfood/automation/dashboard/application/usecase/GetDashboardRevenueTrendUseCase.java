package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.dto.DashboardTrendPointResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver.TrendBucketUnit;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.model.DateBasis;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * "최근 20일/5주/2년/8분기 추이" 전부를 이 UseCase 하나로 처리한다(9장 revenue-trend API).
 * DAY/WEEK 버킷은 주문일자 기준, MONTH/QUARTER 버킷은 출고일 기준을 쓴다(원본 화면 구성과 동일).
 */
@Service
public class GetDashboardRevenueTrendUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardRevenueTrendUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public List<DashboardTrendPointResponse> execute(TrendBucketUnit unit, int count, LocalDate endDate) {
        return execute(unit, count, endDate, null);
    }

    /** dateBasis를 지정하면 버킷 단위 기반 기본 규칙 대신 명시한 날짜 기준을 강제한다(예: 월간 탭의 일별 출고 추이). */
    public List<DashboardTrendPointResponse> execute(TrendBucketUnit unit, int count, LocalDate endDate, DateBasis dateBasis) {
        List<DashboardPeriodResolver.DateRange> buckets = periodResolver.resolveTrendBuckets(unit, count, endDate);
        boolean useShippedDate = dateBasis != null
            ? dateBasis == DateBasis.SHIPPED
            : (unit == TrendBucketUnit.MONTH || unit == TrendBucketUnit.QUARTER);

        return buckets.stream()
            .map(range -> {
                DashboardSummary summary = useShippedDate
                    ? dashboardQueryRepository.summaryByShippedDate(range.start(), range.end())
                    : dashboardQueryRepository.summaryByOrderDate(range.start(), range.end());
                return new DashboardTrendPointResponse(range.start(), range.end(), summary.revenue(), summary.orderCount());
            })
            .toList();
    }
}
