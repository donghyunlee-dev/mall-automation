import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import useCommonStatusConfig from './useCommonStatusConfig';
import {
  getCommonStatusConfig,
  updateCommonStatusConfig,
} from '../api/commonStatusConfig';
import type { CommonStatusConfig } from '../types/commonStatusConfig';

vi.mock('../api/commonStatusConfig', () => ({
  getCommonStatusConfig: vi.fn(),
  updateCommonStatusConfig: vi.fn(),
}));

const createConfig = (
  overrides: Partial<CommonStatusConfig> = {},
): CommonStatusConfig => ({
  scope: 'GENERAL',
  outOfStockQty: 20,
  onSaleQty: 21,
  safetyRate: 50,
  remainValidDays: 150,
  ...overrides,
});

describe('useCommonStatusConfig', () => {
  it('loads common status config on mount', async () => {
    vi.mocked(getCommonStatusConfig).mockResolvedValue([
      createConfig(),
      createConfig({
        scope: 'EXCLUSIVE',
        outOfStockQty: 1,
        onSaleQty: 2,
      }),
    ]);

    const { result } = renderHook(() => useCommonStatusConfig());

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.configs).toEqual([
      createConfig(),
      createConfig({
        scope: 'EXCLUSIVE',
        outOfStockQty: 1,
        onSaleQty: 2,
      }),
    ]);
  });

  it('rolls back config when save fails', async () => {
    vi.mocked(getCommonStatusConfig).mockResolvedValue([createConfig()]);
    vi.mocked(updateCommonStatusConfig).mockRejectedValue(new Error('save failed'));

    const { result } = renderHook(() => useCommonStatusConfig());

    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.updateConfig('GENERAL', {
        outOfStockQty: 30,
        onSaleQty: 31,
        safetyRate: 40,
        remainValidDays: 120,
      }).catch(() => undefined);
    });

    await waitFor(() => {
      expect(result.current.configs).toEqual([createConfig()]);
      expect(result.current.error).toBe('상태 기준 저장에 실패했어요. 다시 시도해주세요.');
    });
  });
});
