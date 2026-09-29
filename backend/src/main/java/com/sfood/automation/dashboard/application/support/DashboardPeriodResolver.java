package com.sfood.automation.dashboard.application.support;

import com.sfood.automation.dashboard.domain.model.DashboardPeriod;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * period(일/주/월/분기) + 기준 파라미터를 실제 조회에 쓸 [start, end] 날짜 범위로 변환한다.
 * 대시보드 조회 API 전반(9장 설계의 summary/hourly-revenue/grade-revenue/grade-detail/
 * top-items/revenue-trend/grade-pivot/yoy-comparison)에서 공통으로 재사용한다.
 */
@Component
public class DashboardPeriodResolver {

    public record DateRange(LocalDate start, LocalDate end) {
    }

    /** revenue-trend / grade-pivot(9장)이 공유하는 구간 단위. DAY/WEEK는 주문일자, MONTH/QUARTER는 출고일 기준으로 쓴다. */
    public enum TrendBucketUnit {
        DAY, WEEK, MONTH, QUARTER
    }

    /** endDate로 끝나는 count개의 구간을 오래된 순으로 반환한다("최근 20일/5주/2년/8분기" 전부 이걸로 처리). */
    public List<DateRange> resolveTrendBuckets(TrendBucketUnit unit, int count, LocalDate endDate) {
        require(count > 0, "count는 1 이상이어야 합니다.");
        require(endDate != null, "endDate가 필요합니다.");

        List<DateRange> buckets = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            buckets.add(resolveBucket(unit, endDate, i));
        }
        return buckets;
    }

    private DateRange resolveBucket(TrendBucketUnit unit, LocalDate endDate, int offset) {
        return switch (unit) {
            case DAY -> {
                LocalDate day = endDate.minusDays(offset);
                yield new DateRange(day, day);
            }
            case WEEK -> {
                LocalDate weekEnd = endDate.minusWeeks(offset);
                LocalDate monday = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new DateRange(monday, monday.plusDays(6));
            }
            case MONTH -> {
                YearMonth month = YearMonth.from(endDate).minusMonths(offset);
                yield new DateRange(month.atDay(1), month.atEndOfMonth());
            }
            case QUARTER -> resolveQuarterly(quarterOrdinal(endDate) - offset);
        };
    }

    private int quarterOrdinal(LocalDate date) {
        return date.getYear() * 4 + (date.getMonthValue() - 1) / 3;
    }

    private DateRange resolveQuarterly(int quarterOrdinal) {
        int year = Math.floorDiv(quarterOrdinal, 4);
        int quarter = Math.floorMod(quarterOrdinal, 4) + 1;
        return resolveQuarterly(year, quarter);
    }

    public DateRange resolve(DashboardPeriod period, LocalDate date, YearMonth yearMonth, Integer year, Integer quarter) {
        return switch (period) {
            case DAILY -> resolveDaily(date);
            case WEEKLY -> resolveWeekly(date);
            case MONTHLY -> resolveMonthly(yearMonth);
            case QUARTERLY -> resolveQuarterly(year, quarter);
        };
    }

    private DateRange resolveDaily(LocalDate date) {
        require(date != null, "DAILY 조회에는 date가 필요합니다.");
        return new DateRange(date, date);
    }

    private DateRange resolveWeekly(LocalDate date) {
        require(date != null, "WEEKLY 조회에는 date가 필요합니다.");
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new DateRange(monday, date);
    }

    private DateRange resolveMonthly(YearMonth yearMonth) {
        require(yearMonth != null, "MONTHLY 조회에는 yearMonth가 필요합니다.");
        return new DateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    private DateRange resolveQuarterly(Integer year, Integer quarter) {
        require(year != null && quarter != null, "QUARTERLY 조회에는 year와 quarter가 필요합니다.");
        require(quarter >= 1 && quarter <= 4, "quarter는 1~4 사이여야 합니다.");
        int startMonth = (quarter - 1) * 3 + 1;
        YearMonth firstMonth = YearMonth.of(year, startMonth);
        YearMonth lastMonth = firstMonth.plusMonths(2);
        return new DateRange(firstMonth.atDay(1), lastMonth.atEndOfMonth());
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new DashboardQueryException(message);
        }
    }
}
