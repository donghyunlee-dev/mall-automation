import { describe, expect, it } from 'vitest';
import { twoYearMonthCount } from './twoYearRange';

describe('twoYearMonthCount', () => {
  it('returns 21 for September (Jan of prior year through September)', () => {
    expect(twoYearMonthCount('2026-09')).toBe(21);
  });

  it('returns 13 for January (Jan of prior year through January)', () => {
    expect(twoYearMonthCount('2026-01')).toBe(13);
  });

  it('returns 24 for December (full prior year plus current year through December)', () => {
    expect(twoYearMonthCount('2026-12')).toBe(24);
  });
});
