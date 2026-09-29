import * as XLSX from 'xlsx';
import type { GoodsItem, GoodsStatusLog } from '../types/goods';

export interface GoodsStatusLogExcelRow {
  godoGoodsCode: string;
  erpGoodsCode: string;
  goodsName: string;
  typeLabel: string;
  wmsAvailableQty: number;
  beforeStatusLabel: string;
  afterStatusLabel: string;
  changedAtLabel: string;
}

const STATUS_LABEL: Record<GoodsStatusLog['beforeStatus'], string> = {
  ON_SALE: '판매중',
  OUT_OF_STOCK: '품절',
};

const EXCEL_HEADERS = [
  '변경 일시',
  '고도몰 상품코드',
  'ERP 품목코드',
  '상품명',
  '구분',
  '가용수량',
  '이전 상태',
  '변경 상태',
] as const;

const EXCEL_COLUMN_WIDTHS = [
  { wch: 20 },
  { wch: 18 },
  { wch: 18 },
  { wch: 32 },
  { wch: 10 },
  { wch: 12 },
  { wch: 16 },
  { wch: 20 },
] as const;

const formatDateTime = (dateStr: string): string => {
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return '—';

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}.${month}.${day} ${hours}:${minutes}`;
};

const getTypeLabel = (item?: GoodsItem): string => {
  if (!item) return '—';
  return item.onlyMallYn === 'Y' ? '전용' : '범용';
};

export const buildGoodsStatusLogExcelRows = (
  logs: GoodsStatusLog[],
  allItems: GoodsItem[],
): GoodsStatusLogExcelRow[] => {
  const goodsMap = new Map(allItems.map((item) => [item.godoGoodsCode, item]));

  return [...logs]
    .sort((a, b) => new Date(b.changedAt).getTime() - new Date(a.changedAt).getTime())
    .map((log) => {
      const goods = goodsMap.get(log.godoGoodsCode);
      return {
        godoGoodsCode: log.godoGoodsCode,
        erpGoodsCode: log.erpGoodsCode,
        goodsName: goods?.goodsName ?? '—',
        typeLabel: getTypeLabel(goods),
        wmsAvailableQty: log.wmsAvailableQty,
        beforeStatusLabel: STATUS_LABEL[log.beforeStatus],
        afterStatusLabel: STATUS_LABEL[log.afterStatus],
        changedAtLabel: formatDateTime(log.changedAt),
      };
    });
};

export const getGoodsStatusLogExcelFileName = (now: Date = new Date()): string => {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  return `상품변경이력_${year}-${month}-${day}_${hours}${minutes}.xlsx`;
};

export const downloadGoodsStatusLogsExcel = (
  logs: GoodsStatusLog[],
  allItems: GoodsItem[],
  now: Date = new Date(),
): string => {
  const rows = buildGoodsStatusLogExcelRows(logs, allItems);
  const worksheetData = [
    [...EXCEL_HEADERS],
    ...rows.map((row) => [
      row.changedAtLabel,
      row.godoGoodsCode,
      row.erpGoodsCode,
      row.goodsName,
      row.typeLabel,
      row.wmsAvailableQty,
      row.beforeStatusLabel,
      row.afterStatusLabel,
    ]),
  ];

  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);
  worksheet['!cols'] = [...EXCEL_COLUMN_WIDTHS];
  worksheet['!autofilter'] = { ref: 'A1:H1' };
  XLSX.utils.book_append_sheet(workbook, worksheet, '상품 변경 이력');

  const fileName = getGoodsStatusLogExcelFileName(now);
  XLSX.writeFile(workbook, fileName);
  return fileName;
};
