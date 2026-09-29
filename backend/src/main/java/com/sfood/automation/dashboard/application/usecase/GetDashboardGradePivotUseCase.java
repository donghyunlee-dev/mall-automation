package com.sfood.automation.dashboard.application.usecase;

import com.sfood.automation.dashboard.application.dto.GradePivotResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver.DateRange;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver.TrendBucketUnit;
import com.sfood.automation.dashboard.domain.model.DateBasis;
import com.sfood.automation.dashboard.domain.model.GradeDetail;
import com.sfood.automation.dashboard.domain.model.GradePivotMetric;
import com.sfood.automation.dashboard.domain.port.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 등급×기간 매트릭스(일별/월별/분기별 실적 상세, 9장 grade-pivot API). DAY/WEEK는 주문일자, MONTH/QUARTER는 출고일 기준. */
@Service
public class GetDashboardGradePivotUseCase {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final DashboardPeriodResolver periodResolver;

    public GetDashboardGradePivotUseCase(DashboardQueryRepository dashboardQueryRepository, DashboardPeriodResolver periodResolver) {
        this.dashboardQueryRepository = dashboardQueryRepository;
        this.periodResolver = periodResolver;
    }

    public GradePivotResponse execute(GradePivotMetric metric, TrendBucketUnit unit, int count, LocalDate endDate) {
        return execute(metric, unit, count, endDate, null);
    }

    /** dateBasis를 지정하면 버킷 단위 기반 기본 규칙 대신 명시한 날짜 기준을 강제한다(예: 월간 탭의 일별 출고 실적 상세). */
    public GradePivotResponse execute(GradePivotMetric metric, TrendBucketUnit unit, int count, LocalDate endDate, DateBasis dateBasis) {
        List<DateRange> buckets = periodResolver.resolveTrendBuckets(unit, count, endDate);
        boolean useShippedDate = dateBasis != null
            ? dateBasis == DateBasis.SHIPPED
            : (unit == TrendBucketUnit.MONTH || unit == TrendBucketUnit.QUARTER);

        Map<String, List<BigDecimal>> byGrade = new LinkedHashMap<>();
        List<LocalDate> bucketEnds = new ArrayList<>();

        for (int i = 0; i < buckets.size(); i++) {
            DateRange range = buckets.get(i);
            bucketEnds.add(range.end());
            List<GradeDetail> details = useShippedDate
                ? dashboardQueryRepository.gradeDetailByShippedDate(range.start(), range.end())
                : dashboardQueryRepository.gradeDetailByOrderDate(range.start(), range.end());

            for (GradeDetail detail : details) {
                List<BigDecimal> row = byGrade.computeIfAbsent(detail.grade(), grade -> zeroFilledRow(buckets.size()));
                row.set(i, metricValue(metric, detail));
            }
        }

        return new GradePivotResponse(bucketEnds, byGrade);
    }

    private List<BigDecimal> zeroFilledRow(int size) {
        List<BigDecimal> row = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            row.add(BigDecimal.ZERO);
        }
        return row;
    }

    private BigDecimal metricValue(GradePivotMetric metric, GradeDetail detail) {
        return switch (metric) {
            case REVENUE -> detail.revenue();
            case ORDER_COUNT -> BigDecimal.valueOf(detail.orderCount());
            case BUYER_COUNT -> BigDecimal.valueOf(detail.buyerCount());
            case AVG_SKU -> BigDecimal.valueOf(detail.avgSku());
            case AVG_QTY -> BigDecimal.valueOf(detail.avgQty());
            case AOV -> detail.aov();
            case ARPPU -> detail.arppu();
        };
    }
}
