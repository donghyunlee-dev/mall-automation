package com.sfood.automation.dashboard.domain.model;

import java.math.BigDecimal;

public record TopItem(String productName, BigDecimal revenue, long qty) {
}
