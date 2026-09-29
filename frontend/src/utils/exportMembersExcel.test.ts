import { describe, expect, it } from 'vitest';
import { getMembersExcelFileName, buildMemberTemplateRow, TEMPLATE_HEADERS } from './exportMembersExcel';
import type { MemberDetail } from '../types/member';

const sample: MemberDetail = {
  memNo: 38568,
  memId: 'grace7503',
  memName: '김은혜',
  groupSno: 9,
  groupName: 'B2B 거래처',
  appFl: 'y',
  appDt: '2025-01-20',
  email: 'grace@example.com',
  cellPhone: '010-1234-5678',
  phone: null,
  address: '06000 서울시 강남구',
  entryDt: '2025-01-15',
  lastLoginDt: '2026-06-01',
  entryPath: 'pc',
  maillingFl: 'y',
  smsFl: 'y',
  nickName: null,
  sexFl: 'w',
  birthDt: null,
  marriFl: null,
  marriDate: null,
  job: null,
  interest: null,
  loginCount: 12,
  saleAmount: 50000,
  mileage: 1500,
  deposit: 0,
  recommId: null,
  ex1: '카페',
  ex2: '발행필요',
  ex3: null,
  ex4: null,
  ex5: null,
  ex6: null,
  memo: null,
  business: {
    company: '에이바우트커피',
    businessNo: '797-26-01495',
    ceoName: '김은혜',
    serviceCode: '56',
    serviceName: '휴게음식점',
    itemCode: '01',
    itemName: '서비스업',
    comAddress: '39207 경상북도 구미시',
    bizLicenseFile: null,
    duplicateMemNo: null,
  },
  partner: {
    memberStatus: 'NORMAL' as const,
    partnerCode: '990954',
    partnerName: '(에쓰푸드몰)에이바우트커피',
    taxInvoiceFl: 'n' as const,
    updatedAt: '2026-06-11T14:30:00',
  },
};

describe('exportMembersExcel', () => {
  it('파일명을 그룹웨어 거래처등록 업로드파일_날짜.xlsx 형식으로 생성', () => {
    const fixed = new Date(2026, 5, 23, 9, 5);
    expect(getMembersExcelFileName(fixed)).toBe('그룹웨어 거래처등록 업로드파일_2026-06-23.xlsx');
  });

  it('템플릿 53컬럼 구조로 행 생성', () => {
    const row = buildMemberTemplateRow(sample);
    expect(TEMPLATE_HEADERS).toHaveLength(53);
    expect(row).toHaveLength(53);
  });

  it('거래처 상태 INACTIVE 회원은 거래처등록 컬럼에 불용 표시', () => {
    const inactive = { ...sample, partner: { ...sample.partner!, memberStatus: 'INACTIVE' as const } };
    const row = buildMemberTemplateRow(inactive);
    expect(row[1]).toBe('불용');
  });

  it('상세 데이터를 템플릿 컬럼 위치에 매핑', () => {
    const row = buildMemberTemplateRow(sample);
    expect(row[0]).toBe('grace7503'); // 아이디
    expect(row[1]).toBe('O'); // 거래처등록 (NORMAL → O)
    expect(row[2]).toBe('990954'); // 거래처코드
    expect(row[5]).toBeNull(); // 세금계산서여부 (n → null)
    expect(row[8]).toBe(38568); // 회원번호
    expect(row[11]).toBe('승인'); // 가입승인여부 (y)
    expect(row[18]).toBe('김은혜'); // 이름 (평문)
    expect(row[20]).toBe('010-1234-5678'); // 휴대폰 (평문)
    expect(row[30]).toBe('797-26-01495'); // 사업자번호
    expect(row[32]).toBe('휴게음식점'); // 업태명
    expect(row[33]).toBe('56'); // 업태코드
    expect(row[34]).toBe('서비스업'); // 업종명
    expect(row[35]).toBe('01'); // 업종코드
    expect(row[47]).toBe('카페'); // 추가정보1 (ex1)
  });
});
