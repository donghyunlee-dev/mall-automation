package com.sfood.automation.dashboard.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendPointResponse(LocalDate start, LocalDate end, BigDecimal revenue, long orderCount) {
}
