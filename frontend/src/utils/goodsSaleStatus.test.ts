import { describe, expect, it } from 'vitest';
import type { GoodsItem } from '../types/goods';
import {
  isSaleStatusOnSale,
  isSaleStatusSoldOut,
  isSaleStatusUnavailable,
} from './goodsSaleStatus';

const createItem = (
  overrides: Partial<GoodsItem> = {},
): GoodsItem => ({
  id: 1,
  godoGoodsCode: '1000000504',
  erpGoodsCode: '171101109Z',
  goodsName: '테스트 상품',
  onlyMallYn: 'N',
  soldOutYn: 'N',
  availableQty: 20,
  safetyRate: 40,
  remainValidDays: 150,
  godoSyncYn: 'Y',
  erpSyncYn: 'Y',
  godoSyncBlockedYn: 'N',
  message: null,
  status: 'NORMAL',
  ...overrides,
});

describe('goodsSaleStatus', () => {
  it('treats goods as unavailable only when godoSyncYn is N', () => {
    expect(
      isSaleStatusUnavailable(createItem({ godoSyncYn: 'N', erpSyncYn: 'Y' })),
    ).toBe(true);
    expect(
      isSaleStatusUnavailable(createItem({ godoSyncYn: 'Y', erpSyncYn: 'N' })),
    ).toBe(false);
  });

  it('treats goods as sold out when godoSyncYn is Y and soldOutYn is Y', () => {
    expect(
      isSaleStatusSoldOut(createItem({ godoSyncYn: 'Y', soldOutYn: 'Y', erpSyncYn: 'N' })),
    ).toBe(true);
  });

  it('treats goods as on sale when godoSyncYn is Y and soldOutYn is N', () => {
    expect(
      isSaleStatusOnSale(createItem({ godoSyncYn: 'Y', soldOutYn: 'N', erpSyncYn: 'N' })),
    ).toBe(true);
  });
});
