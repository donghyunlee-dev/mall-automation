package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.dto.DashboardSummaryResponse;
import com.sfood.automation.dashboard.application.dto.YoyComparisonResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver.DateRange;
import com.sfood.automation.dashboard.application.support.DashboardQueryException;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.YearMonth;
import org.springframework.stereotype.Service;

/** 전년 동월/동분기 비교(9장 yoy-comparison API). 월간/분기 탭에만 존재하는 기능이라 DAILY/WEEKLY는 지원하지 않는다. */
@Service
public class GetDashboardYoyComparisonUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardYoyComparisonUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public YoyComparisonResponse execute(DashboardPeriod period, YearMonth yearMonth, Integer year, Integer quarter) {
        DateRange currentRange = periodResolver.resolve(period, null, yearMonth, year, quarter);
        DateRange previousRange = switch (period) {
            case MONTHLY -> periodResolver.resolve(period, null, yearMonth.minusYears(1), null, null);
            case QUARTERLY -> periodResolver.resolve(period, null, null, year - 1, quarter);
            case DAILY, WEEKLY -> throw new DashboardQueryException("전년 동기 비교는 MONTHLY/QUARTERLY만 지원합니다.");
        };

        DashboardSummary current = dashboardQueryRepository.summaryByShippedDate(currentRange.start(), currentRange.end());
        DashboardSummary previous = dashboardQueryRepository.summaryByShippedDate(previousRange.start(), previousRange.end());

        return new YoyComparisonResponse(DashboardSummaryResponse.from(current), DashboardSummaryResponse.from(previous));
    }
}
