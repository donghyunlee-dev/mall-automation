import client from './client';
import type {
  DashboardPeriodParams,
  DashboardSummary,
  DashboardTrendPoint,
  DateBasis,
  GradeDetail,
  GradePivotMetric,
  GradePivotResponse,
  GradeRevenue,
  HourlyRevenue,
  TopItem,
  TrendBucketUnit,
  YoyComparisonResponse,
} from '../types/dashboard';

export const getDashboardSummary = async (params: DashboardPeriodParams): Promise<DashboardSummary> => {
  const { data } = await client.get<DashboardSummary>('/api/dashboard/summary', { params });
  return data;
};

export const getHourlyRevenue = async (date: string): Promise<HourlyRevenue[]> => {
  const { data } = await client.get<HourlyRevenue[]>('/api/dashboard/hourly-revenue', { params: { date } });
  return data;
};

export const getGradeRevenue = async (params: DashboardPeriodParams): Promise<GradeRevenue[]> => {
  const { data } = await client.get<GradeRevenue[]>('/api/dashboard/grade-revenue', { params });
  return data;
};

export const getGradeDetail = async (params: DashboardPeriodParams): Promise<GradeDetail[]> => {
  const { data } = await client.get<GradeDetail[]>('/api/dashboard/grade-detail', { params });
  return data;
};

export const getTopItems = async (params: DashboardPeriodParams & { limit?: number }): Promise<TopItem[]> => {
  const { data } = await client.get<TopItem[]>('/api/dashboard/top-items', { params });
  return data;
};

export const getJambongBreakdown = async (params: DashboardPeriodParams): Promise<TopItem[]> => {
  const { data } = await client.get<TopItem[]>('/api/dashboard/jambong-breakdown', { params });
  return data;
};

export const getRevenueTrend = async (
  bucket: TrendBucketUnit,
  count: number,
  endDate: string,
  dateBasis?: DateBasis,
): Promise<DashboardTrendPoint[]> => {
  const { data } = await client.get<DashboardTrendPoint[]>('/api/dashboard/revenue-trend', {
    params: { bucket, count, endDate, dateBasis },
  });
  return data;
};

export const getGradePivot = async (
  metric: GradePivotMetric,
  bucket: TrendBucketUnit,
  count: number,
  endDate: string,
  dateBasis?: DateBasis,
): Promise<GradePivotResponse> => {
  const { data } = await client.get<GradePivotResponse>('/api/dashboard/grade-pivot', {
    params: { metric, bucket, count, endDate, dateBasis },
  });
  return data;
};

export const getYoyComparison = async (
  params: Pick<DashboardPeriodParams, 'period' | 'yearMonth' | 'year' | 'quarter'>,
): Promise<YoyComparisonResponse> => {
  const { data } = await client.get<YoyComparisonResponse>('/api/dashboard/yoy-comparison', { params });
  return data;
};
