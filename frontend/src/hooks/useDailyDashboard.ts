import { useCallback, useEffect, useState } from 'react';
import {
  getDashboardSummary,
  getGradeDetail,
  getGradeRevenue,
  getHourlyRevenue,
  getJambongBreakdown,
  getRevenueTrend,
  getTopItems,
} from '../api/dashboard';
import type {
  DashboardSummary,
  DashboardTrendPoint,
  GradeDetail,
  GradeRevenue,
  HourlyRevenue,
  TopItem,
} from '../types/dashboard';

interface DailyDashboardData {
  summary: DashboardSummary | null;
  hourlyRevenue: HourlyRevenue[];
  gradeRevenue: GradeRevenue[];
  gradeDetail: GradeDetail[];
  topItems: TopItem[];
  jambongBreakdown: TopItem[];
  trend20Days: DashboardTrendPoint[];
}

interface UseDailyDashboardReturn extends DailyDashboardData {
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

const EMPTY_DATA: DailyDashboardData = {
  summary: null,
  hourlyRevenue: [],
  gradeRevenue: [],
  gradeDetail: [],
  topItems: [],
  jambongBreakdown: [],
  trend20Days: [],
};

const useDailyDashboard = (date: string): UseDailyDashboardReturn => {
  const [data, setData] = useState<DailyDashboardData>(EMPTY_DATA);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async (targetDate: string) => {
    setLoading(true);
    setError(null);
    try {
      const [summary, hourlyRevenue, gradeRevenue, gradeDetail, topItems, jambongBreakdown, trend20Days] = await Promise.all([
        getDashboardSummary({ period: 'DAILY', date: targetDate }),
        getHourlyRevenue(targetDate),
        getGradeRevenue({ period: 'DAILY', date: targetDate }),
        getGradeDetail({ period: 'DAILY', date: targetDate }),
        getTopItems({ period: 'DAILY', date: targetDate, limit: 6 }),
        getJambongBreakdown({ period: 'DAILY', date: targetDate }),
        getRevenueTrend('DAY', 20, targetDate),
      ]);
      setData({ summary, hourlyRevenue, gradeRevenue, gradeDetail, topItems, jambongBreakdown, trend20Days });
    } catch {
      setError('대시보드 데이터를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchAll(date);
  }, [date, fetchAll]);

  return { ...data, loading, error, refetch: () => void fetchAll(date) };
};

export default useDailyDashboard;
