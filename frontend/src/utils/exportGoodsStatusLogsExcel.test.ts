import { describe, expect, it } from 'vitest';
import type { GoodsItem, GoodsStatusLog } from '../types/goods';
import {
  buildGoodsStatusLogExcelRows,
  getGoodsStatusLogExcelFileName,
} from './exportGoodsStatusLogsExcel';

const goodsItems: GoodsItem[] = [
  {
    id: 1,
    godoGoodsCode: 'GODO-001',
    erpGoodsCode: 'ERP-001',
    goodsName: '상품 A',
    onlyMallYn: 'N',
    soldOutYn: 'N',
    availableQty: 20,
    safetyRate: 5,
    remainValidDays: 150,
    godoSyncYn: 'Y',
    erpSyncYn: 'Y',
    godoSyncBlockedYn: 'N',
    message: null,
    status: 'NORMAL',
  },
  {
    id: 2,
    godoGoodsCode: 'GODO-002',
    erpGoodsCode: 'ERP-002',
    goodsName: '상품 B',
    onlyMallYn: 'Y',
    soldOutYn: 'Y',
    availableQty: 0,
    safetyRate: 10,
    remainValidDays: 60,
    godoSyncYn: 'Y',
    erpSyncYn: 'Y',
    godoSyncBlockedYn: 'N',
    message: null,
    status: 'NORMAL',
  },
];

const logs: GoodsStatusLog[] = [
  {
    id: 10,
    godoGoodsCode: 'GODO-001',
    erpGoodsCode: 'ERP-001',
    wmsAvailableQty: 8,
    criteriaQty: 10,
    safetyRate: 5,
    validPercent: 80,
    remainValidDays: null,
    beforeStatus: 'ON_SALE',
    afterStatus: 'OUT_OF_STOCK',
    changedAt: '2026-04-27T13:10:00+09:00',
    createdAt: '2026-04-27T13:10:00+09:00',
  },
  {
    id: 11,
    godoGoodsCode: 'GODO-002',
    erpGoodsCode: 'ERP-002',
    wmsAvailableQty: 13,
    criteriaQty: 15,
    safetyRate: 10,
    validPercent: 86.7,
    remainValidDays: null,
    beforeStatus: 'OUT_OF_STOCK',
    afterStatus: 'ON_SALE',
    changedAt: '2026-04-28T09:05:00+09:00',
    createdAt: '2026-04-28T09:05:00+09:00',
  },
  {
    id: 12,
    godoGoodsCode: 'GODO-999',
    erpGoodsCode: 'ERP-999',
    wmsAvailableQty: 1,
    criteriaQty: 2,
    safetyRate: 5,
    validPercent: 50,
    remainValidDays: null,
    beforeStatus: 'ON_SALE',
    afterStatus: 'OUT_OF_STOCK',
    changedAt: '2026-04-26T18:30:00+09:00',
    createdAt: '2026-04-26T18:30:00+09:00',
  },
];

describe('exportGoodsStatusLogsExcel', () => {
  it('builds rows from the full queried logs and enriches with goods data', () => {
    const rows = buildGoodsStatusLogExcelRows(logs, goodsItems);

    expect(rows).toEqual([
      {
        godoGoodsCode: 'GODO-002',
        erpGoodsCode: 'ERP-002',
        goodsName: '상품 B',
        typeLabel: '전용',
        wmsAvailableQty: 13,
        beforeStatusLabel: '품절',
        afterStatusLabel: '판매중',
        changedAtLabel: '2026.04.28 09:05',
      },
      {
        godoGoodsCode: 'GODO-001',
        erpGoodsCode: 'ERP-001',
        goodsName: '상품 A',
        typeLabel: '범용',
        wmsAvailableQty: 8,
        beforeStatusLabel: '판매중',
        afterStatusLabel: '품절',
        changedAtLabel: '2026.04.27 13:10',
      },
      {
        godoGoodsCode: 'GODO-999',
        erpGoodsCode: 'ERP-999',
        goodsName: '—',
        typeLabel: '—',
        wmsAvailableQty: 1,
        beforeStatusLabel: '판매중',
        afterStatusLabel: '품절',
        changedAtLabel: '2026.04.26 18:30',
      },
    ]);
  });

  it('builds the history excel file name with timestamp', () => {
    expect(getGoodsStatusLogExcelFileName(new Date('2026-04-28T14:35:00+09:00'))).toBe(
      '상품변경이력_2026-04-28_1435.xlsx',
    );
  });
});
