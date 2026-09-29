package com.sfood.automation.dashboard.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GradePivotResponse(List<LocalDate> bucketEnds, Map<String, List<BigDecimal>> byGrade) {
}
