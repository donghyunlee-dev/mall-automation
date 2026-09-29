import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import useGoods from './useGoods';
import { getGoods } from '../api/goods';
import type { GoodsItem } from '../types/goods';

vi.mock('../api/goods', () => ({
  getGoods: vi.fn(),
}));

const createItem = (overrides: Partial<GoodsItem> = {}): GoodsItem => ({
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
  createdId: 'tester@s-food.com',
  createdAt: '2026-04-06T00:00:00Z',
  updatedId: 'tester@s-food.com',
  updatedAt: '2026-04-06T00:00:00Z',
  ...overrides,
});

describe('useGoods', () => {
  it('keeps godo-synced goods in sale status filters even when ERP sync is N', async () => {
    vi.mocked(getGoods).mockResolvedValue({
      items: [
        createItem({ id: 1, godoSyncYn: 'Y', erpSyncYn: 'N', soldOutYn: 'N', goodsName: '판매중 상품' }),
        createItem({ id: 2, godoSyncYn: 'Y', erpSyncYn: 'N', soldOutYn: 'Y', goodsName: '품절 상품' }),
        createItem({ id: 3, godoSyncYn: 'N', erpSyncYn: 'Y', soldOutYn: 'N', goodsName: '확인불가 상품' }),
      ],
    });

    const { result } = renderHook(() => useGoods());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.setStatusFilter('on');
    });
    expect(result.current.filteredItems.map((item) => item.goodsName)).toEqual(['판매중 상품']);

    act(() => {
      result.current.setStatusFilter('out');
    });
    expect(result.current.filteredItems.map((item) => item.goodsName)).toEqual(['품절 상품']);

    act(() => {
      result.current.setStatusFilter('unavailable');
    });
    expect(result.current.filteredItems.map((item) => item.goodsName)).toEqual(['확인불가 상품']);
  });

  it('filters blocked goods when block filter is enabled', async () => {
    vi.mocked(getGoods).mockResolvedValue({
      items: [
        createItem({ id: 1, goodsName: '허용 상품', godoSyncBlockedYn: 'N' }),
        createItem({ id: 2, goodsName: '차단 상품', godoSyncBlockedYn: 'Y' }),
        createItem({ id: 3, goodsName: '미검증 차단값 상품', godoSyncBlockedYn: 'Y', erpSyncYn: 'N' }),
      ],
    });

    const { result } = renderHook(() => useGoods());

    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.setBlockFilter('blocked');
    });

    expect(result.current.filteredItems.map((item) => item.goodsName)).toEqual(['차단 상품']);
  });
});
