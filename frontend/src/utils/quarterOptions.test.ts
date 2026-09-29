import { describe, expect, it } from 'vitest';
import { generateQuarterOptions } from './quarterOptions';

describe('generateQuarterOptions', () => {
  it('returns quarters from end to start, descending', () => {
    expect(generateQuarterOptions({ year: 2025, quarter: 1 }, { year: 2025, quarter: 3 })).toEqual([
      { year: 2025, quarter: 3 },
      { year: 2025, quarter: 2 },
      { year: 2025, quarter: 1 },
    ]);
  });

  it('crosses a year boundary correctly', () => {
    expect(generateQuarterOptions({ year: 2025, quarter: 4 }, { year: 2026, quarter: 1 })).toEqual([
      { year: 2026, quarter: 1 },
      { year: 2025, quarter: 4 },
    ]);
  });

  it('returns a single quarter when start equals end', () => {
    expect(generateQuarterOptions({ year: 2026, quarter: 3 }, { year: 2026, quarter: 3 })).toEqual([
      { year: 2026, quarter: 3 },
    ]);
  });
});
