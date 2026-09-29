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
import { quarterEndDate } from '../utils/quarterEndDate';

const MONTHLY_PIVOT_METRICS: GradePivotMetric[] = ['REVENUE', 'ORDER_COUNT', 'BUYER_COUNT'];
const QUARTERLY_PIVOT_METRICS: GradePivotMetric[] = [
  'REVENUE',
  'ORDER_COUNT',
  'BUYER_COUNT',
  'AVG_QTY',
  'AVG_SKU',
  'AOV',
  'ARPPU',
];
const EIGHT_QUARTERS = 8;

interface QuarterlyDashboardData {
  summary: DashboardSummary | null;
  monthlyTrend: DashboardTrendPoint[];
  gradeRevenue: GradeRevenue[];
  gradeDetail: GradeDetail[];
  topItems: TopItem[];
  jambongBreakdown: TopItem[];
  monthlyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>>;
  monthlyPivotLabels: string[];
  yoyComparison: YoyComparisonResponse | null;
  eightQuarterTrend: DashboardTrendPoint[];
  quarterlyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>>;
  quarterlyPivotLabels: string[];
}

interface UseQuarterlyDashboardReturn extends QuarterlyDashboardData {
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

const EMPTY_DATA: QuarterlyDashboardData = {
  summary: null,
  monthlyTrend: [],
  gradeRevenue: [],
  gradeDetail: [],
  topItems: [],
  jambongBreakdown: [],
  monthlyPivot: {},
  monthlyPivotLabels: [],
  yoyComparison: null,
  eightQuarterTrend: [],
  quarterlyPivot: {},
  quarterlyPivotLabels: [],
};

const useQuarterlyDashboard = (year: number, quarter: number): UseQuarterlyDashboardReturn => {
  const [data, setData] = useState<QuarterlyDashboardData>(EMPTY_DATA);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async (targetYear: number, targetQuarter: number) => {
    setLoading(true);
    setError(null);
    try {
      const endDate = quarterEndDate(targetYear, targetQuarter);

      const [
        summary,
        monthlyTrend,
        gradeRevenue,
        gradeDetail,
        topItems,
        jambongBreakdown,
        monthlyPivotResults,
        yoyComparison,
        eightQuarterTrend,
        quarterlyPivotResults,
      ] = await Promise.all([
        getDashboardSummary({ period: 'QUARTERLY', year: targetYear, quarter: targetQuarter }),
        getRevenueTrend('MONTH', 3, endDate),
        getGradeRevenue({ period: 'QUARTERLY', year: targetYear, quarter: targetQuarter }),
        getGradeDetail({ period: 'QUARTERLY', year: targetYear, quarter: targetQuarter }),
        getTopItems({ period: 'QUARTERLY', year: targetYear, quarter: targetQuarter, limit: 6 }),
        getJambongBreakdown({ period: 'QUARTERLY', year: targetYear, quarter: targetQuarter }),
        Promise.all(MONTHLY_PIVOT_METRICS.map((metric) => getGradePivot(metric, 'MONTH', 3, endDate))),
        getYoyComparison({ period: 'QUARTERLY', year: targetYear, quarter: targetQuarter }),
        getRevenueTrend('QUARTER', EIGHT_QUARTERS, endDate),
        Promise.all(
          QUARTERLY_PIVOT_METRICS.map((metric) => getGradePivot(metric, 'QUARTER', EIGHT_QUARTERS, endDate)),
        ),
      ]);

      const monthlyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>> = {};
      MONTHLY_PIVOT_METRICS.forEach((metric, i) => {
        monthlyPivot[metric] = monthlyPivotResults[i];
      });

      const quarterlyPivot: Partial<Record<GradePivotMetric, GradePivotResponse>> = {};
      QUARTERLY_PIVOT_METRICS.forEach((metric, i) => {
        quarterlyPivot[metric] = quarterlyPivotResults[i];
      });

      setData({
        summary,
        monthlyTrend,
        gradeRevenue,
        gradeDetail,
        topItems,
        jambongBreakdown,
        monthlyPivot,
        monthlyPivotLabels: monthlyPivotResults[0]?.bucketEnds ?? [],
        yoyComparison,
        eightQuarterTrend,
        quarterlyPivot,
        quarterlyPivotLabels: quarterlyPivotResults[0]?.bucketEnds ?? [],
      });
    } catch {
      setError('대시보드 데이터를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchAll(year, quarter);
  }, [year, quarter, fetchAll]);

  return { ...data, loading, error, refetch: () => void fetchAll(year, quarter) };
};

export default useQuarterlyDashboard;
