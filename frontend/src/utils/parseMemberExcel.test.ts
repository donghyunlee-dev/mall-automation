import { describe, expect, it, vi, beforeEach } from 'vitest';
import { parseMemberExcel } from './parseMemberExcel';

const mockRead = vi.fn();
const mockSheetToJson = vi.fn();

vi.mock('xlsx', () => ({
  read: (...args: unknown[]) => mockRead(...args),
  utils: {
    sheet_to_json: (...args: unknown[]) => mockSheetToJson(...args),
  },
}));

function makeFile(name = 'test.xlsx'): File {
  return new File(['dummy'], name, { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
}

beforeEach(() => {
  vi.clearAllMocks();
  mockRead.mockReturnValue({ SheetNames: ['Sheet1'], Sheets: { Sheet1: {} } });
});

describe('parseMemberExcel', () => {
  it('첫 번째 헤더가 "회원번호" → type = LIST', async () => {
    mockSheetToJson.mockReturnValue([
      ['회원번호', '등록일'],
      [12345, '2025-01-15'],
    ]);

    const result = await parseMemberExcel(makeFile());
    expect(result.type).toBe('LIST');
    expect(result.rows).toHaveLength(1);
    expect(result.isDamaged).toBe(false);
  });

  it('첫 번째 헤더가 "아이디" → type = TEMPLATE', async () => {
    mockSheetToJson.mockReturnValue([
      ['아이디', '거래처등록'],
      ['hong123', 'O'],
    ]);

    const result = await parseMemberExcel(makeFile());
    expect(result.type).toBe('TEMPLATE');
    expect(result.isDamaged).toBe(false);
  });

  it('rows.length === 0 → isDamaged = true', async () => {
    mockSheetToJson.mockReturnValue([['회원번호']]);

    const result = await parseMemberExcel(makeFile());
    expect(result.isDamaged).toBe(true);
    expect(result.rows).toHaveLength(0);
  });
});
