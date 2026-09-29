import { useCallback, useEffect, useState } from 'react';
import {
  getDashboardSummary,
  getGradeDetail,
  getGradePivot,
  getGradeRevenue,
  getJambongBreakdown,
  getRevenueTrend,
  getTopItems,
  getYoyComparison,
} from '../api/dashboard';
import type {
  DashboardSummary,
  DashboardTrendPoint,
  GradeDetail,
  GradePivotMetric,
  GradePivotResponse,
  GradeRevenue,
  TopItem,
  YoyComparisonResponse,
} from '../types/dashboard';
import { monthEndDate, monthlyTrendRange } from '../utils/monthlyTrendRange';
import { twoYearMonthCount } from '../utils/twoYearRange';

const DAILY_PIVOT_METRICS: GradePivotMetric[] = ['REVENUE', 'ORDER_COUNT', 'BUYER_COUNT'];
const YEARLY_PIVOT_METRICS: GradePivotMetric[] = [
  'REVENUE',
  'ORDER_COUNT',
  'BUYER_COUNT',
  'AVG_QTY',
  'AVG_SKU',
  'AOV',
  'ARPPU',
];

interface MonthlyDashboardData {
  summary: DashboardSummary | null;
  dailyTrend: DashboardTrendPoint[];
  gradeRevenue: GradeRevenue[];
  gradeDetail: GradeDetail[];
  topItems: TopItem[];
  jambongBreakdown: TopItem[];
  dailyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>>;
  dailyPivotLabels: string[];
  yoyComparison: YoyComparisonResponse | null;
  yearlyTrend: DashboardTrendPoint[];
  yearlyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>>;
  yearlyPivotLabels: string[];
}

interface UseMonthlyDashboardReturn extends MonthlyDashboardData {
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

const EMPTY_DATA: MonthlyDashboardData = {
  summary: null,
  dailyTrend: [],
  gradeRevenue: [],
  gradeDetail: [],
  topItems: [],
  jambongBreakdown: [],
  dailyPivot: {},
  dailyPivotLabels: [],
  yoyComparison: null,
  yearlyTrend: [],
  yearlyPivot: {},
  yearlyPivotLabels: [],
};

const sumAcrossGrades = (byGrade: Record<string, number[]>, index: number): number =>
  Object.values(byGrade).reduce((sum, values) => sum + values[index], 0);

/** referenceDate: 이번 달 조회 시 일별 출고 추이를 어제까지로 제한하기 위한 기준일(yyyy-MM-dd). */
const useMonthlyDashboard = (yearMonth: string, referenceDate: string): UseMonthlyDashboardReturn => {
  const [data, setData] = useState<MonthlyDashboardData>(EMPTY_DATA);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async (targetYearMonth: string, targetReferenceDate: string) => {
    setLoading(true);
    setError(null);
    try {
      const dailyRange = monthlyTrendRange(targetYearMonth, targetReferenceDate);
      const yearlyEndDate = monthEndDate(targetYearMonth);
      const yearlyCount = twoYearMonthCount(targetYearMonth);

      const [
        summary,
        dailyTrend,
        gradeRevenue,
        gradeDetail,
        topItems,
        jambongBreakdown,
        dailyPivotResults,
        yoyComparison,
        yearlyPivotResults,
      ] = await Promise.all([
        getDashboardSummary({ period: 'MONTHLY', yearMonth: targetYearMonth }),
        getRevenueTrend('DAY', dailyRange.count, dailyRange.end, 'SHIPPED'),
        getGradeRevenue({ period: 'MONTHLY', yearMonth: targetYearMonth }),
        getGradeDetail({ period: 'MONTHLY', yearMonth: targetYearMonth }),
        getTopItems({ period: 'MONTHLY', yearMonth: targetYearMonth, limit: 6 }),
        getJambongBreakdown({ period: 'MONTHLY', yearMonth: targetYearMonth }),
        Promise.all(
          DAILY_PIVOT_METRICS.map((metric) =>
            getGradePivot(metric, 'DAY', dailyRange.count, dailyRange.end, 'SHIPPED'),
          ),
        ),
        getYoyComparison({ period: 'MONTHLY', yearMonth: targetYearMonth }),
        Promise.all(YEARLY_PIVOT_METRICS.map((metric) => getGradePivot(metric, 'MONTH', yearlyCount, yearlyEndDate))),
      ]);

      const dailyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>> = {};
      DAILY_PIVOT_METRICS.forEach((metric, i) => {
        dailyPivot[metric] = dailyPivotResults[i];
      });

      const yearlyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>> = {};
      YEARLY_PIVOT_METRICS.forEach((metric, i) => {
        yearlyPivot[metric] = yearlyPivotResults[i];
      });

      const revenuePivot = yearlyPivot.REVENUE;
      const orderCountPivot = yearlyPivot.ORDER_COUNT;
      const yearlyTrend: DashboardTrendPoint[] =
        revenuePivot && orderCountPivot
          ? revenuePivot.bucketEnds.map((end, i) => ({
              start: end,
              end,
              revenue: sumAcrossGrades(revenuePivot.byGrade, i),
              orderCount: sumAcrossGrades(orderCountPivot.byGrade, i),
            }))
          : [];

      setData({
        summary,
        dailyTrend,
        gradeRevenue,
        gradeDetail,
        topItems,
        jambongBreakdown,
        dailyPivot,
        dailyPivotLabels: dailyPivotResults[0]?.bucketEnds ?? [],
        yoyComparison,
        yearlyTrend,
        yearlyPivot,
        yearlyPivotLabels: revenuePivot?.bucketEnds ?? [],
      });
    } catch {
      setError('대시보드 데이터를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchAll(yearMonth, referenceDate);
  }, [yearMonth, referenceDate, fetchAll]);

  return { ...data, loading, error, refetch: () => void fetchAll(yearMonth, referenceDate) };
};

export default useMonthlyDashboard;
