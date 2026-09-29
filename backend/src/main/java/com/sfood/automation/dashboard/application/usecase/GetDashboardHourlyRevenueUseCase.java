package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.domain.model.HourlyRevenue;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardHourlyRevenueUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;

    public GetDashboardHourlyRevenueUseCase(DashboardQueryRepository dashboardQueryRepository) {
        this.dashboardQueryRepository = dashboardQueryRepository;
    }

    public List<HourlyRevenue> execute(LocalDate date) {
        return dashboardQueryRepository.hourlyRevenue(date);
    }
}
