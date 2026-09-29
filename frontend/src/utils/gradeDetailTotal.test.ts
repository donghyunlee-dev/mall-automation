import { describe, expect, it } from 'vitest';
import { sumGradeDetails } from './gradeDetailTotal';
import type { GradeDetail } from '../types/dashboard';

const detail = (grade: string, revenue: number, orderCount: number, buyerCount: number): GradeDetail => ({
  grade,
  revenue,
  orderCount,
  buyerCount,
  avgSku: 1,
  avgQty: 1,
  aov: 1,
  arppu: 1,
});

describe('sumGradeDetails', () => {
  it('sums revenue, orderCount, buyerCount across grades', () => {
    const result = sumGradeDetails([detail('VIP', 10000, 2, 2), detail('일반회원', 5000, 1, 1)]);

    expect(result).toEqual({ revenue: 15000, orderCount: 3, buyerCount: 3 });
  });

  it('returns zeros for an empty list', () => {
    expect(sumGradeDetails([])).toEqual({ revenue: 0, orderCount: 0, buyerCount: 0 });
  });
});
