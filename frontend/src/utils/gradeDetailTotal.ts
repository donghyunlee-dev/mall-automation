import type { GradeDetail } from '../types/dashboard';

export interface GradeDetailTotal {
  revenue: number;
  orderCount: number;
  buyerCount: number;
}

/** 등급별 상세 테이블 하단 합계 행. 평균 지표(SKU/수량/AOV/ARPPU)는 원본과 동일하게 합산하지 않고 표시하지 않는다. */
export const sumGradeDetails = (details: GradeDetail[]): GradeDetailTotal =>
  details.reduce(
    (acc, d) => ({
      revenue: acc.revenue + d.revenue,
      orderCount: acc.orderCount + d.orderCount,
      buyerCount: acc.buyerCount + d.buyerCount,
    }),
    { revenue: 0, orderCount: 0, buyerCount: 0 },
  );
