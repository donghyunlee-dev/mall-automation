import { describe, expect, it } from 'vitest';
import { daysSinceMonday } from './weekTrend';

describe('daysSinceMonday', () => {
  it('returns 1 when the date is Monday', () => {
    expect(daysSinceMonday('2026-08-31')).toBe(1);
  });

  it('returns 4 when the date is Thursday', () => {
    expect(daysSinceMonday('2026-09-03')).toBe(4);
  });

  it('returns 7 when the date is Sunday', () => {
    expect(daysSinceMonday('2026-09-06')).toBe(7);
  });
});
