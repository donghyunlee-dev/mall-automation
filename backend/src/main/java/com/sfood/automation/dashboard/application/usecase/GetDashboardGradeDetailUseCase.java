package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.dto.GradeDetailResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardGradeDetailUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardGradeDetailUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public List<GradeDetailResponse> execute(DashboardPeriod period, LocalDate date, YearMonth yearMonth, Integer year, Integer quarter) {
        DashboardPeriodResolver.DateRange range = periodResolver.resolve(period, date, yearMonth, year, quarter);
        var details = switch (period) {
            case DAILY, WEEKLY -> dashboardQueryRepository.gradeDetailByOrderDate(range.start(), range.end());
            case MONTHLY, QUARTERLY -> dashboardQueryRepository.gradeDetailByShippedDate(range.start(), range.end());
        };
        return details.stream().map(GradeDetailResponse::from).toList();
    }
}
