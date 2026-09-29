import { describe, expect, it } from 'vitest';
import { monthEndDate, monthlyTrendRange } from './monthlyTrendRange';

describe('monthlyTrendRange', () => {
  it('uses the full month when the reference date is after month end', () => {
    expect(monthlyTrendRange('2026-08', '2026-09-03')).toEqual({
      start: '2026-08-01',
      end: '2026-08-31',
      count: 31,
    });
  });

  it('clamps the end date to the reference date for the current month', () => {
    expect(monthlyTrendRange('2026-09', '2026-09-03')).toEqual({
      start: '2026-09-01',
      end: '2026-09-03',
      count: 3,
    });
  });

  it('returns count 1 when the reference date is the first day of the month', () => {
    expect(monthlyTrendRange('2026-09', '2026-09-01')).toEqual({
      start: '2026-09-01',
      end: '2026-09-01',
      count: 1,
    });
  });
});

describe('monthEndDate', () => {
  it('returns the last day of the given month', () => {
    expect(monthEndDate('2026-02')).toBe('2026-02-28');
    expect(monthEndDate('2026-09')).toBe('2026-09-30');
  });
});
