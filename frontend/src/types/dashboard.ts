export type DashboardPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';
export type TrendBucketUnit = 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER';
export type DateBasis = 'ORDER' | 'SHIPPED';
export type GradePivotMetric =
  | 'REVENUE'
  | 'ORDER_COUNT'
  | 'BUYER_COUNT'
  | 'AVG_SKU'
  | 'AVG_QTY'
  | 'AOV'
  | 'ARPPU';

export interface DashboardPeriodParams {
  period: DashboardPeriod;
  date?: string; // yyyy-MM-dd (DAILY/WEEKLY)
  yearMonth?: string; // yyyy-MM (MONTHLY)
  year?: number; // QUARTERLY
  quarter?: number; // QUARTERLY (1~4)
}

export interface DashboardSummary {
  revenue: number;
  orderCount: number;
  buyerCount: number;
  orderFrequency: number;
  avgSku: number;
  avgQty: number;
  aov: number;
  arppu: number;
}

export interface HourlyRevenue {
  hour: number;
  revenue: number;
}

export interface GradeRevenue {
  grade: string;
  revenue: number;
}

export interface GradeDetail {
  grade: string;
  revenue: number;
  orderCount: number;
  buyerCount: number;
  avgSku: number;
  avgQty: number;
  aov: number;
  arppu: number;
}

export interface TopItem {
  productName: string;
  revenue: number;
  qty: number;
}

export interface DashboardTrendPoint {
  start: string;
  end: string;
  revenue: number;
  orderCount: number;
}

export interface GradePivotResponse {
  bucketEnds: string[];
  byGrade: Record<string, number[]>;
}

export interface YoyComparisonResponse {
  current: DashboardSummary;
  previous: DashboardSummary;
}
