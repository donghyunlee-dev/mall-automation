package com.sfood.automation.batch.domain.enums;

public enum BatchJobType {
    PRODUCT_SYNC("product-sync"),
    MEMBER_SYNC("member-sync"),
    ORDER_SYNC("order-sync");

    private final String jobName;

    BatchJobType(String jobName) {
        this.jobName = jobName;
    }

    public String jobName() {
        return jobName;
    }
}
