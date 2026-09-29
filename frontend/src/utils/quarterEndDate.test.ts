import { describe, expect, it } from 'vitest';
import { quarterEndDate } from './quarterEndDate';

describe('quarterEndDate', () => {
  it('returns the last day of Q1 (March 31)', () => {
    expect(quarterEndDate(2026, 1)).toBe('2026-03-31');
  });

  it('returns the last day of Q4 (December 31)', () => {
    expect(quarterEndDate(2026, 4)).toBe('2026-12-31');
  });

  it('returns the last day of Q2, handling 30-day months', () => {
    expect(quarterEndDate(2026, 2)).toBe('2026-06-30');
  });
});
