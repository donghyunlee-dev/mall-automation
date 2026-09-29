import * as XLSX from 'xlsx';
import type { MemberDetail } from '../types/member';

// godomall-member-analysys-template.xlsx 기반 헤더 (업태·업종을 명/코드로 분리 → 53컬럼)
export const TEMPLATE_HEADERS = [
  '아이디', '거래처등록', '거래처코드', '거래처명', '거래처명', '세금계산서여부', '중복여부 확인', '파일명',
  '회원번호', '등록일', '가입승인일', '가입승인여부', '가입경로', '최종로그인일', '회원구분', '등급',
  '아이디', '닉네임', '이름', '이메일', '휴대폰번호', '전화번호', '주소', '메일수신여부', 'SMS수신여부',
  '주문금액', '마일리지', '예치금', '방문횟수', '상호', '사업자번호', '대표자명', '업태명', '업태코드', '업종명', '업종코드', '사업자 주소',
  '팩스 번호', '추천인아이디', '성별', '생일', '결혼여부', '결혼기념일', '직업', '관심분야', '개인정보유효기간',
  '남기는말씀', '추가정보1', '추가정보2', '추가정보3', '추가정보4', '추가정보5', '추가정보6',
] as const;

const flag = (v: 'y' | 'n' | null | undefined): string | null => (v === 'y' ? 'O' : v === 'n' ? null : null);

// MemberDetail → 분석 템플릿 53컬럼 1행
export const buildMemberTemplateRow = (m: MemberDetail): (string | number | null)[] => {
  const b = m.business;
  const p = m.partner;
  return [
    m.memId,                                  // 0 아이디
    p?.memberStatus === 'NORMAL' ? 'O' : p?.memberStatus === 'INACTIVE' ? '불용' : null,  // 1 거래처등록
    p?.partnerCode ?? null,                   // 2 거래처코드
    p?.partnerName ?? null,                   // 3 거래처명
    p?.partnerName ?? null,                   // 4 거래처명(중복)
    flag(p?.taxInvoiceFl),                    // 5 세금계산서여부
    null,                                     // 6 중복여부 확인
    b?.bizLicenseFile ?? null,                // 7 파일명
    m.memNo,                                  // 8 회원번호
    m.entryDt ?? null,                        // 9 등록일
    m.appDt ?? null,                          // 10 가입승인일
    m.appFl === 'y' ? '승인' : '미승인',       // 11 가입승인여부
    m.entryPath ?? null,                      // 12 가입경로
    m.lastLoginDt ?? null,                    // 13 최종로그인일
    b ? 'business' : 'personal',              // 14 회원구분
    m.groupSno,                               // 15 등급
    m.memId,                                  // 16 아이디
    m.nickName ?? null,                       // 17 닉네임
    m.memName ?? null,                        // 18 이름
    m.email ?? null,                          // 19 이메일
    m.cellPhone ?? null,                      // 20 휴대폰번호
    m.phone ?? null,                          // 21 전화번호
    m.address ?? null,                        // 22 주소
    m.maillingFl ?? null,                     // 23 메일수신여부
    m.smsFl ?? null,                          // 24 SMS수신여부
    m.saleAmount ?? 0,                        // 25 주문금액
    `${m.mileage ?? 0}점`,                    // 26 마일리지
    `${m.deposit ?? 0}원`,                    // 27 예치금
    m.loginCount ?? 0,                        // 28 방문횟수
    b?.company ?? null,                       // 29 상호
    b?.businessNo ?? null,                    // 30 사업자번호
    b?.ceoName ?? null,                       // 31 대표자명
    b?.serviceName ?? null,                   // 32 업태명
    b?.serviceCode ?? null,                   // 33 업태코드
    b?.itemName ?? null,                      // 34 업종명
    b?.itemCode ?? null,                      // 35 업종코드
    b?.comAddress ?? null,                    // 36 사업자 주소
    null,                                     // 37 팩스 번호 (상세 응답 없음)
    m.recommId ?? null,                       // 38 추천인아이디
    m.sexFl ?? null,                          // 39 성별
    m.birthDt ?? null,                        // 40 생일
    m.marriFl ?? null,                        // 41 결혼여부
    m.marriDate ?? null,                      // 42 결혼기념일
    m.job ?? null,                            // 43 직업
    m.interest ?? null,                       // 44 관심분야
    null,                                     // 45 개인정보유효기간 (상세 응답 없음)
    m.memo ?? null,                           // 46 남기는말씀
    m.ex1 ?? null,                            // 47 추가정보1
    m.ex2 ?? null,                            // 48 추가정보2
    m.ex3 ?? null,                            // 49 추가정보3
    m.ex4 ?? null,                            // 50 추가정보4
    m.ex5 ?? null,                            // 51 추가정보5
    m.ex6 ?? null,                            // 52 추가정보6
  ];
};

export const getMembersExcelFileName = (now: Date = new Date()): string => {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `그룹웨어 거래처등록 업로드파일_${year}-${month}-${day}.xlsx`;
};

export const downloadMembersExcel = (members: MemberDetail[], now: Date = new Date()): string => {
  const worksheetData = [[...TEMPLATE_HEADERS], ...members.map(buildMemberTemplateRow)];

  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);
  const lastCol = XLSX.utils.encode_col(TEMPLATE_HEADERS.length - 1);
  worksheet['!autofilter'] = { ref: `A1:${lastCol}1` };
  XLSX.utils.book_append_sheet(workbook, worksheet, '회원 상세');

  const fileName = getMembersExcelFileName(now);
  XLSX.writeFile(workbook, fileName);
  return fileName;
};
