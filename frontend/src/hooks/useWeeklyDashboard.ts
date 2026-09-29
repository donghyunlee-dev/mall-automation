import { useCallback, useEffect, useState } from 'react';
import {
  getDashboardSummary,
  getGradeDetail,
  getGradeRevenue,
  getJambongBreakdown,
  getRevenueTrend,
  getTopItems,
} from '../api/dashboard';
import type { DashboardSummary, DashboardTrendPoint, GradeDetail, GradeRevenue, TopItem } from '../types/dashboard';
import { daysSinceMonday } from '../utils/weekTrend';

interface WeeklyDashboardData {
  summary: DashboardSummary | null;
  weekTrend: DashboardTrendPoint[];
  gradeRevenue: GradeRevenue[];
  gradeDetail: GradeDetail[];
  topItems: TopItem[];
  jambongBreakdown: TopItem[];
  trend5Weeks: DashboardTrendPoint[];
}

interface UseWeeklyDashboardReturn extends WeeklyDashboardData {
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

const EMPTY_DATA: WeeklyDashboardData = {
  summary: null,
  weekTrend: [],
  gradeRevenue: [],
  gradeDetail: [],
  topItems: [],
  jambongBreakdown: [],
  trend5Weeks: [],
};

const useWeeklyDashboard = (date: string): UseWeeklyDashboardReturn => {
  const [data, setData] = useState<WeeklyDashboardData>(EMPTY_DATA);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAll = useCallback(async (targetDate: string) => {
    setLoading(true);
    setError(null);
    try {
      const [summary, weekTrend, gradeRevenue, gradeDetail, topItems, jambongBreakdown, trend5Weeks] = await Promise.all([
        getDashboardSummary({ period: 'WEEKLY', date: targetDate }),
        getRevenueTrend('DAY', daysSinceMonday(targetDate), targetDate),
        getGradeRevenue({ period: 'WEEKLY', date: targetDate }),
        getGradeDetail({ period: 'WEEKLY', date: targetDate }),
        getTopItems({ period: 'WEEKLY', date: targetDate, limit: 6 }),
        getJambongBreakdown({ period: 'WEEKLY', date: targetDate }),
        getRevenueTrend('WEEK', 5, targetDate),
      ]);
      setData({ summary, weekTrend, gradeRevenue, gradeDetail, topItems, jambongBreakdown, trend5Weeks });
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

export default useWeeklyDashboard;
