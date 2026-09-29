import * as XLSX from 'xlsx';
import type { MemberUploadType } from '../types/member';

export interface ParseResult {
  type: MemberUploadType;
  rows: (string | number | null)[][];
  isDamaged: boolean;
}

export function parseMemberExcel(file: File): Promise<ParseResult> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = e.target?.result;
        const wb = XLSX.read(data, { type: 'array' });
        const ws = wb.Sheets[wb.SheetNames[0]];
        const all = XLSX.utils.sheet_to_json<(string | number | null)[]>(ws, {
          header: 1,
          defval: null,
        }) as (string | number | null)[][];

        const headerRow = all[0] ?? [];
        const firstCell = String(headerRow[0] ?? '').trim();

        let type: MemberUploadType;
        if (firstCell === '회원번호') {
          type = 'LIST';
        } else if (firstCell === '아이디') {
          type = 'TEMPLATE';
        } else {
          console.warn('parseMemberExcel: 파일 종류 판별 실패, LIST로 처리합니다.');
          type = 'LIST';
        }

        const rows = all.slice(1) as (string | number | null)[][];

        resolve({ type, rows, isDamaged: rows.length === 0 });
      } catch (err) {
        reject(err instanceof Error ? err : new Error('파일을 파싱할 수 없어요.'));
      }
    };
    reader.onerror = () => reject(new Error('파일을 읽을 수 없어요.'));
    reader.readAsArrayBuffer(file);
  });
}
