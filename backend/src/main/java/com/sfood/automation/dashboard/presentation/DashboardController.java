package com.sfood.automation.dashboard.presentation;

import com.sfood.automation.dashboard.application.dto.DashboardSummaryResponse;
import com.sfood.automation.dashboard.application.dto.DashboardTrendPointResponse;
import com.sfood.automation.dashboard.application.dto.GradeDetailResponse;
import com.sfood.automation.dashboard.application.dto.GradePivotResponse;
import com.sfood.automation.dashboard.application.dto.YoyComparisonResponse;
import com.sfood.automation.dashboard.application.support.DashboardPeriodResolver.TrendBucketUnit;
import com.sfood.automation.dashboard.application.usecase.GetDashboardGradeDetailUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardGradePivotUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardGradeRevenueUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardHourlyRevenueUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardJambongBreakdownUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardRevenueTrendUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardSummaryUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardTopItemsUseCase;
import com.sfood.automation.dashboard.application.usecase.GetDashboardYoyComparisonUseCase;
import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import com.sfood.automation.dashboard.domain.model.DateBasis;
import com.sfood.automation.dashboard.domain.model.GradePivotMetric;
import com.sfood.automation.dashboard.domain.model.GradeRevenue;
import com.sfood.automation.dashboard.domain.model.HourlyRevenue;
import com.sfood.automation.dashboard.domain.model.TopItem;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;
    private final GetDashboardHourlyRevenueUseCase getDashboardHourlyRevenueUseCase;
    private final GetDashboardGradeRevenueUseCase getDashboardGradeRevenueUseCase;
    private final GetDashboardGradeDetailUseCase getDashboardGradeDetailUseCase;
    private final GetDashboardTopItemsUseCase getDashboardTopItemsUseCase;
    private final GetDashboardRevenueTrendUseCase getDashboardRevenueTrendUseCase;
    private final GetDashboardGradePivotUseCase getDashboardGradePivotUseCase;
    private final GetDashboardYoyComparisonUseCase getDashboardYoyComparisonUseCase;
    private final GetDashboardJambongBreakdownUseCase getDashboardJambongBreakdownUseCase;

    public DashboardController(
        GetDashboardSummaryUseCase getDashboardSummaryUseCase,
        GetDashboardHourlyRevenueUseCase getDashboardHourlyRevenueUseCase,
        GetDashboardGradeRevenueUseCase getDashboardGradeRevenueUseCase,
        GetDashboardGradeDetailUseCase getDashboardGradeDetailUseCase,
        GetDashboardTopItemsUseCase getDashboardTopItemsUseCase,
        GetDashboardRevenueTrendUseCase getDashboardRevenueTrendUseCase,
        GetDashboardGradePivotUseCase getDashboardGradePivotUseCase,
        GetDashboardYoyComparisonUseCase getDashboardYoyComparisonUseCase,
        GetDashboardJambongBreakdownUseCase getDashboardJambongBreakdownUseCase
    ) {
        this.getDashboardSummaryUseCase = getDashboardSummaryUseCase;
        this.getDashboardHourlyRevenueUseCase = getDashboardHourlyRevenueUseCase;
        this.getDashboardGradeRevenueUseCase = getDashboardGradeRevenueUseCase;
        this.getDashboardGradeDetailUseCase = getDashboardGradeDetailUseCase;
        this.getDashboardTopItemsUseCase = getDashboardTopItemsUseCase;
        this.getDashboardRevenueTrendUseCase = getDashboardRevenueTrendUseCase;
        this.getDashboardGradePivotUseCase = getDashboardGradePivotUseCase;
        this.getDashboardYoyComparisonUseCase = getDashboardYoyComparisonUseCase;
        this.getDashboardJambongBreakdownUseCase = getDashboardJambongBreakdownUseCase;
    }

    @GetMapping("/summary")
    DashboardSummaryResponse summary(
        @RequestParam DashboardPeriod period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer quarter
    ) {
        return getDashboardSummaryUseCase.execute(period, date, parseYearMonth(yearMonth), year, quarter);
    }

    @GetMapping("/hourly-revenue")
    List<HourlyRevenue> hourlyRevenue(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return getDashboardHourlyRevenueUseCase.execute(date);
    }

    @GetMapping("/grade-revenue")
    List<GradeRevenue> gradeRevenue(
        @RequestParam DashboardPeriod period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer quarter
    ) {
        return getDashboardGradeRevenueUseCase.execute(period, date, parseYearMonth(yearMonth), year, quarter);
    }

    @GetMapping("/grade-detail")
    List<GradeDetailResponse> gradeDetail(
        @RequestParam DashboardPeriod period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer quarter
    ) {
        return getDashboardGradeDetailUseCase.execute(period, date, parseYearMonth(yearMonth), year, quarter);
    }

    @GetMapping("/top-items")
    List<TopItem> topItems(
        @RequestParam DashboardPeriod period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer quarter,
        @RequestParam(defaultValue = "6") int limit
    ) {
        return getDashboardTopItemsUseCase.execute(period, date, parseYearMonth(yearMonth), year, quarter, limit);
    }

    @GetMapping("/revenue-trend")
    List<DashboardTrendPointResponse> revenueTrend(
        @RequestParam TrendBucketUnit bucket,
        @RequestParam int count,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) DateBasis dateBasis
    ) {
        return dateBasis == null
            ? getDashboardRevenueTrendUseCase.execute(bucket, count, endDate)
            : getDashboardRevenueTrendUseCase.execute(bucket, count, endDate, dateBasis);
    }

    @GetMapping("/grade-pivot")
    GradePivotResponse gradePivot(
        @RequestParam GradePivotMetric metric,
        @RequestParam TrendBucketUnit bucket,
        @RequestParam int count,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) DateBasis dateBasis
    ) {
        return dateBasis == null
            ? getDashboardGradePivotUseCase.execute(metric, bucket, count, endDate)
            : getDashboardGradePivotUseCase.execute(metric, bucket, count, endDate, dateBasis);
    }

    @GetMapping("/yoy-comparison")
    YoyComparisonResponse yoyComparison(
        @RequestParam DashboardPeriod period,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer quarter
    ) {
        return getDashboardYoyComparisonUseCase.execute(period, parseYearMonth(yearMonth), year, quarter);
    }

    @GetMapping("/jambong-breakdown")
    List<TopItem> jambongBreakdown(
        @RequestParam DashboardPeriod period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String yearMonth,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer quarter
    ) {
        return getDashboardJambongBreakdownUseCase.execute(period, date, parseYearMonth(yearMonth), year, quarter);
    }

    private YearMonth parseYearMonth(String value) {
        return value == null ? null : YearMonth.parse(value);
    }
}
