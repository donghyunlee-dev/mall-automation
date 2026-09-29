package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.dto.DashboardSummaryResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.DashboardSummary;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardSummaryUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardSummaryUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public DashboardSummaryResponse execute(DashboardPeriod period, LocalDate date, YearMonth yearMonth, Integer year, Integer quarter) {
        DashboardPeriodResolver.DateRange range = periodResolver.resolve(period, date, yearMonth, year, quarter);

        DashboardSummary summary = switch (period) {
            case DAILY, WEEKLY -> dashboardQueryRepository.summaryByOrderDate(range.start(), range.end());
            case MONTHLY, QUARTERLY -> dashboardQueryRepository.summaryByShippedDate(range.start(), range.end());
        };

        return DashboardSummaryResponse.from(summary);
    }
}
