package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.GradeRevenue;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardGradeRevenueUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardGradeRevenueUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public List<GradeRevenue> execute(DashboardPeriod period, LocalDate date, YearMonth yearMonth, Integer year, Integer quarter) {
        DashboardPeriodResolver.DateRange range = periodResolver.resolve(period, date, yearMonth, year, quarter);
        return switch (period) {
            case DAILY, WEEKLY -> dashboardQueryRepository.gradeRevenueByOrderDate(range.start(), range.end());
            case MONTHLY, QUARTERLY -> dashboardQueryRepository.gradeRevenueByShippedDate(range.start(), range.end());
        };
    }
}
