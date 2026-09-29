import { describe, expect, it } from 'vitest';
import { generateMonthOptions } from './monthOptions';

describe('generateMonthOptions', () => {
  it('returns months from end to start, descending', () => {
    expect(generateMonthOptions('2025-01', '2025-03')).toEqual(['2025-03', '2025-02', '2025-01']);
  });

  it('crosses a year boundary correctly', () => {
    expect(generateMonthOptions('2025-11', '2026-01')).toEqual(['2026-01', '2025-12', '2025-11']);
  });

  it('returns a single month when start equals end', () => {
    expect(generateMonthOptions('2026-09', '2026-09')).toEqual(['2026-09']);
  });
});
