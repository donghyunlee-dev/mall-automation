import { describe, expect, it } from 'vitest';
import type { GoodsItem } from '../types/goods';
import { buildGoodsExcelRows, getGoodsExcelFileName } from './exportGoodsExcel';

const createItem = (overrides: Partial<GoodsItem> = {}): GoodsItem => ({
  id: 1,
  godoGoodsCode: '1000000504',
  erpGoodsCode: '171101109Z',
  goodsName: '테스트 상품',
  onlyMallYn: 'N',
  soldOutYn: 'N',
  availableQty: 20,
  safetyRate: 50,
  remainValidDays: 150,
  godoSyncYn: 'Y',
  erpSyncYn: 'Y',
  godoSyncBlockedYn: 'N',
  message: null,
  status: 'NORMAL',
  createdId: 'creator@s-food.com',
  createdAt: '2026-04-07T00:00:00Z',
  updatedId: 'editor@s-food.com',
  updatedAt: '2026-04-08T00:00:00Z',
  ...overrides,
});

describe('exportGoodsExcel', () => {
  it('builds excel rows from full goods items', () => {
    const rows = buildGoodsExcelRows([
      createItem(),
      createItem({
        id: 2,
        onlyMallYn: 'Y',
        godoSyncYn: 'N',
        erpSyncYn: 'N',
        updatedAt: null,
        updatedId: null,
      }),
    ]);

    expect(rows).toEqual([
      {
        godoGoodsCode: '1000000504',
        erpGoodsCode: '171101109Z',
        goodsName: '테스트 상품',
        typeLabel: '범용',
        saleStatusLabel: '판매중',
        codeValidationLabel: '검증완료',
        editDateLabel: '2026.04.08',
        editorLabel: 'editor',
      },
      {
        godoGoodsCode: '1000000504',
        erpGoodsCode: '171101109Z',
        goodsName: '테스트 상품',
        typeLabel: '전용',
        saleStatusLabel: '확인불가',
        codeValidationLabel: '고도몰/ERP X',
        editDateLabel: '2026.04.07',
        editorLabel: 'creator',
      },
    ]);
  });

  it('creates a timestamped excel file name', () => {
    expect(getGoodsExcelFileName(new Date('2026-04-07T14:05:00'))).toBe(
      '상품목록_2026-04-07_1405.xlsx',
    );
  });
});
