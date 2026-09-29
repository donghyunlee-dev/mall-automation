package com.sfood.automation.dashboard.domain.model;

import java.math.BigDecimal;

public record HourlyRevenue(int hour, BigDecimal revenue) {
}
