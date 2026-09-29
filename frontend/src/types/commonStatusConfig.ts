export type CommonStatusConfigScope = 'GENERAL' | 'EXCLUSIVE';

export interface CommonStatusConfig {
  scope: CommonStatusConfigScope;
  outOfStockQty: number;
  onSaleQty: number;
  safetyRate: number;
  remainValidDays: number;
}

export interface UpdateCommonStatusConfigBody {
  outOfStockQty: number;
  onSaleQty: number;
  safetyRate: number;
  remainValidDays: number;
}
