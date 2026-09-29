package com.sfood.automation.dashboard.domain.model;

public record GodoOrderSearchQuery(
    String dateType,
    String startDate,
    String endDate,
    Integer size,
    String lastOrder
) {
    public static GodoOrderSearchQuery byOrderDateRange(String startDate, String endDate) {
        return new GodoOrderSearchQuery("order", startDate, endDate, null, null);
    }
}
