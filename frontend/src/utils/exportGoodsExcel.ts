import * as XLSX from 'xlsx';
import type { GoodsItem } from '../types/goods';
import {
  isSaleStatusOnSale,
  isSaleStatusSoldOut,
  isSaleStatusUnavailable,
} from './goodsSaleStatus';

export interface GoodsExcelRow {
  godoGoodsCode: string;
  erpGoodsCode: string;
  goodsName: string;
  typeLabel: string;
  saleStatusLabel: string;
  codeValidationLabel: string;
  editDateLabel: string;
  editorLabel: string;
}

const EXCEL_HEADERS = [
  '고도몰 상품코드',
  'ERP 품목코드',
  '상품명',
  '구분',
  '판매 상태',
  '코드검증 상태',
  '등록/수정일자',
  '등록/수정자',
] as const;

const EXCEL_COLUMN_WIDTHS = [
  { wch: 18 },
  { wch: 18 },
  { wch: 32 },
  { wch: 10 },
  { wch: 12 },
  { wch: 16 },
  { wch: 16 },
  { wch: 16 },
] as const;

const formatDate = (dateStr: string | null | undefined): string => {
  if (!dateStr) return '—';
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return '—';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
};

const getCodeValidationLabel = (item: GoodsItem): string => {
  if (item.erpSyncYn === 'Y' && item.godoSyncYn === 'Y') return '검증완료';
  if (item.erpSyncYn === 'N' && item.godoSyncYn === 'N') return '고도몰/ERP X';
  if (item.erpSyncYn === 'N') return 'ERP X';
  return '고도몰 X';
};

const getSaleStatusLabel = (item: GoodsItem): string => {
  if (isSaleStatusUnavailable(item)) return '확인불가';
  if (isSaleStatusSoldOut(item)) return '품절';
  if (isSaleStatusOnSale(item)) return '판매중';
  return '—';
};

const getEditorLabel = (item: GoodsItem): string => {
  const editor = item.updatedId ?? item.createdId ?? '';
  if (!editor) return '—';
  return editor.split('@')[0] ?? editor;
};

export const buildGoodsExcelRows = (items: GoodsItem[]): GoodsExcelRow[] =>
  items.map((item) => ({
    godoGoodsCode: item.godoGoodsCode,
    erpGoodsCode: item.erpGoodsCode,
    goodsName: item.goodsName,
    typeLabel: item.onlyMallYn === 'Y' ? '전용' : '범용',
    saleStatusLabel: getSaleStatusLabel(item),
    codeValidationLabel: getCodeValidationLabel(item),
    editDateLabel: item.updatedAt ? formatDate(item.updatedAt) : formatDate(item.createdAt),
    editorLabel: getEditorLabel(item),
  }));

export const getGoodsExcelFileName = (now: Date = new Date()): string => {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  return `상품목록_${year}-${month}-${day}_${hours}${minutes}.xlsx`;
};

export const downloadGoodsExcel = (items: GoodsItem[], now: Date = new Date()): string => {
  const rows = buildGoodsExcelRows(items);
  const worksheetData = [
    [...EXCEL_HEADERS],
    ...rows.map((row) => [
      row.godoGoodsCode,
      row.erpGoodsCode,
      row.goodsName,
      row.typeLabel,
      row.saleStatusLabel,
      row.codeValidationLabel,
      row.editDateLabel,
      row.editorLabel,
    ]),
  ];

  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);
  worksheet['!cols'] = [...EXCEL_COLUMN_WIDTHS];
  worksheet['!autofilter'] = { ref: 'A1:H1' };
  XLSX.utils.book_append_sheet(workbook, worksheet, '전체 상품 목록');

  const fileName = getGoodsExcelFileName(now);
  XLSX.writeFile(workbook, fileName);
  return fileName;
};
