import { describe, expect, it } from 'vitest';
import {
  formatShortDayLabel,
  formatShortMonthLabel,
  formatShortQuarterLabel,
  formatShortWeekLabel,
  formatTrendPointLabel,
  getTrendAxisTitle,
} from './dashboardLabel';

describe('formatShortDayLabel', () => {
  it('formats a Saturday date with 2-digit year and weekday', () => {
    expect(formatShortDayLabel('2026-08-15')).toBe('26-08-15(토)');
  });

  it('formats a Sunday date', () => {
    expect(formatShortDayLabel('2026-09-06')).toBe('26-09-06(일)');
  });
});

describe('formatShortWeekLabel', () => {
  it('formats a week-start date with slashes, no weekday', () => {
    expect(formatShortWeekLabel('2026-08-03')).toBe('26/08/03');
  });
});

describe('formatShortMonthLabel', () => {
  it('abbreviates year to 2 digits from a full date', () => {
    expect(formatShortMonthLabel('2025-01-31')).toBe('25-01');
  });

  it('abbreviates year to 2 digits from a year-month string', () => {
    expect(formatShortMonthLabel('2025-01')).toBe('25-01');
  });
});

describe('formatShortQuarterLabel', () => {
  it('derives Q1 from a quarter-end date', () => {
    expect(formatShortQuarterLabel('2025-03-31')).toBe('25-1Q');
  });

  it('derives Q4 from a quarter-end date', () => {
    expect(formatShortQuarterLabel('2025-12-31')).toBe('25-4Q');
  });
});

describe('formatTrendPointLabel', () => {
  const point = { start: '2026-08-03', end: '2026-08-09', revenue: 0, orderCount: 0 };

  it('uses end date for DAY granularity', () => {
    expect(formatTrendPointLabel('DAY', point)).toBe('26-08-09(일)');
  });

  it('uses start date for WEEK granularity', () => {
    expect(formatTrendPointLabel('WEEK', point)).toBe('26/08/03');
  });

  it('uses end date for MONTH granularity', () => {
    expect(formatTrendPointLabel('MONTH', point)).toBe('26-08');
  });

  it('uses end date for QUARTER granularity', () => {
    expect(formatTrendPointLabel('QUARTER', point)).toBe('26-3Q');
  });
});

describe('getTrendAxisTitle', () => {
  it('returns 일자 for DAY', () => {
    expect(getTrendAxisTitle('DAY')).toBe('일자');
  });

  it('returns 주차 (Week) for WEEK', () => {
    expect(getTrendAxisTitle('WEEK')).toBe('주차 (Week)');
  });

  it('returns 월 for MONTH', () => {
    expect(getTrendAxisTitle('MONTH')).toBe('월');
  });

  it('returns 분기 for QUARTER', () => {
    expect(getTrendAxisTitle('QUARTER')).toBe('분기');
  });
});
