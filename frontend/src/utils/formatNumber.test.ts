import { describe, expect, it } from 'vitest';
import { formatCount, formatDecimal, formatWon } from './formatNumber';

describe('formatWon', () => {
  it('rounds and adds thousand separators with 원 suffix', () => {
    expect(formatWon(1234567.8)).toBe('1,234,568원');
  });
});

describe('formatCount', () => {
  it('rounds and appends the given unit', () => {
    expect(formatCount(12.4, '건')).toBe('12건');
  });
});

describe('formatDecimal', () => {
  it('formats with one decimal place by default', () => {
    expect(formatDecimal(2.567)).toBe('2.6');
  });

  it('formats with the given number of digits', () => {
    expect(formatDecimal(2.567, 2)).toBe('2.57');
  });
});
