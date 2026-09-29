package com.sfood.automation.dashboard.domain.model;

/** revenue-trend/grade-pivot 조회 시 버킷 단위(DAY/WEEK/MONTH/QUARTER)와 무관하게 날짜 기준을 명시적으로 강제할 때 쓴다. */
public enum DateBasis {
    ORDER,
    SHIPPED
}
