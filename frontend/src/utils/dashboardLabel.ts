import type { DashboardTrendPoint } from '../types/dashboard';

export type TrendGranularity = 'DAY' | 'WEEK' | 'MONTH' | 'QUARTER';

const WEEKDAY_KR = ['일', '월', '화', '수', '목', '금', '토'];

const shortYear = (year: string): string => year.slice(2);

export const formatShortDayLabel = (dateStr: string): string => {
  const [year, month, day] = dateStr.split('-');
  const weekday = WEEKDAY_KR[new Date(`${dateStr}T00:00:00`).getDay()];
  return `${shortYear(year)}-${month}-${day}(${weekday})`;
};

export const formatShortWeekLabel = (weekStartDateStr: string): string => {
  const [year, month, day] = weekStartDateStr.split('-');
  return `${shortYear(year)}/${month}/${day}`;
};

export const formatShortMonthLabel = (dateStr: string): string => {
  const [year, month] = dateStr.split('-');
  return `${shortYear(year)}-${month}`;
};

export const formatShortQuarterLabel = (dateStr: string): string => {
  const [year, month] = dateStr.split('-');
  const quarter = Math.floor((Number(month) - 1) / 3) + 1;
  return `${shortYear(year)}-${quarter}Q`;
};

export const formatTrendPointLabel = (granularity: TrendGranularity, point: DashboardTrendPoint): string => {
  switch (granularity) {
    case 'DAY':
      return formatShortDayLabel(point.end);
    case 'WEEK':
      return formatShortWeekLabel(point.start);
    case 'MONTH':
      return formatShortMonthLabel(point.end);
    case 'QUARTER':
      return formatShortQuarterLabel(point.end);
  }
};

const TREND_AXIS_TITLE: Record<TrendGranularity, string> = {
  DAY: '일자',
  WEEK: '주차 (Week)',
  MONTH: '월',
  QUARTER: '분기',
};

export const getTrendAxisTitle = (granularity: TrendGranularity): string => TREND_AXIS_TITLE[granularity];
