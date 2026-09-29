// 실 백엔드 시드용 TEMPLATE 엑셀 생성기
// 사용: node gen-sample-members.mjs  →  sample-members.xlsx 생성
// 생성된 파일을 앱의 [거래처 등록 관리 → 엑셀 업로드]로 업로드하면
// 백엔드가 암호화·해시·upsert를 수행한다 (raw SQL 시드는 암호화가 깨져 불가).
import * as XLSX from 'xlsx';

const GROUP_NAMES = { 1: '일반회원', 2: '사업자회원_화이트', 3: '사업자회원_로제', 5: '사업자회원_레드', 6: '사업자회원_VIP', 9: 'B2B 거래처' };
const FAMILY = [
  ['김', 'kim'], ['이', 'lee'], ['박', 'park'], ['최', 'choi'], ['정', 'jung'],
  ['강', 'kang'], ['조', 'cho'], ['윤', 'yoon'], ['장', 'jang'], ['임', 'lim'],
  ['한', 'han'], ['오', 'oh'], ['서', 'seo'], ['신', 'shin'], ['권', 'kwon'],
  ['황', 'hwang'], ['안', 'ahn'], ['송', 'song'], ['전', 'jeon'], ['홍', 'hong'],
];
const GIVEN = ['민준', '서연', '도윤', '지우', '예준', '하은', '시우', '지호', '주원', '지민',
  '현우', '수빈', '준서', '지윤', '건우', '채원', '우진', '다은', '선우', '유진'];
const SERVICES = ['도매', '소매', '제조', '유통', '음식점', '프랜차이즈'];
const ITEMS = ['식품', '농산물', '수산물', '축산물', '가공식품', '음료'];
const PATHS = ['pc', 'mobile', 'app'];

const daysAgoStr = (n) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
};
const pickGroup = (i) => (i < 40 ? 1 : i < 55 ? 2 : i < 65 ? 3 : i < 75 ? 5 : i < 80 ? 6 : 9);
const pickDaysAgo = (i) => {
  const m = i % 10;
  if (m === 0) return 0;
  if (m <= 3) return m;
  if (m <= 5) return 8 + (i % 18);
  return 40 + i * 8;
};

// TEMPLATE 51컬럼 헤더 (인덱스 0 = '아이디' → 파서가 TEMPLATE로 판별)
const HEADER = [
  '아이디', '거래처등록', '거래처코드', '거래처명', '거래처명(중복)', '세금계산서여부', '중복여부확인', '사업자등록증파일명',
  '회원번호', '등록일', '가입승인일', '가입승인여부', '가입경로', '최종로그인일', '회원구분', '등급',
  '아이디(LIST)', '닉네임', '이름', '이메일', '휴대폰번호', '전화번호', '주소', '메일수신여부', 'SMS수신여부',
  '주문금액', '마일리지', '예치금', '방문횟수', '상호', '사업자번호', '대표자명', '업태', '종목', '사업자주소',
  '팩스번호', '추천인아이디', '성별', '생일', '결혼여부', '결혼기념일', '직업', '관심분야', '개인정보유효기간',
  '남기는말씀', '추가정보1', '추가정보2', '추가정보3', '추가정보4', '추가정보5', '추가정보6',
];

const rows = [HEADER];

for (let i = 0; i < 100; i++) {
  const memNo = 10001 + i;
  const [famKo, famEn] = FAMILY[i % FAMILY.length];
  const memName = famKo + GIVEN[(i * 7) % GIVEN.length];
  const memId = famEn + memNo;
  const groupSno = pickGroup(i);
  const isBiz = groupSno !== 1;
  const email = `${famEn}${memNo}@example.com`;
  const cellPhone = `010-${String(1000 + i).slice(-4)}-${String(5678 + i).slice(-4)}`;
  const entryDt = `${daysAgoStr(pickDaysAgo(i))} 00:00:00`;
  const appFl = i % 9 === 4 ? 'n' : 'y';
  const hasPartner = groupSno === 9 || (isBiz && i % 3 === 0);

  rows.push([
    memId,                                                   // 0 아이디(중복)
    hasPartner ? 'O' : '',                                   // 1 거래처등록
    hasPartner ? 990000 + i : null,                          // 2 거래처코드
    hasPartner ? `(에쓰푸드몰)${memName}거래처` : null,       // 3 거래처명
    hasPartner ? `(에쓰푸드몰)${memName}거래처` : null,       // 4 거래처명(중복)
    hasPartner && i % 2 === 0 ? 'O' : '',                    // 5 세금계산서여부
    hasPartner ? '확인완료' : null,                          // 6 중복여부확인
    isBiz ? `license_${memId}.pdf` : null,                   // 7 사업자등록증파일명
    memNo,                                                   // 8 회원번호
    entryDt,                                                 // 9 등록일
    appFl === 'y' ? entryDt : null,                          // 10 가입승인일
    appFl,                                                   // 11 가입승인여부
    PATHS[i % PATHS.length],                                 // 12 가입경로
    i % 5 === 0 ? null : `${daysAgoStr(i % 30)} 09:00:00`,   // 13 최종로그인일
    isBiz ? 'business' : 'personal',                         // 14 회원구분
    groupSno,                                                // 15 등급
    memId,                                                   // 16 아이디
    null,                                                    // 17 닉네임
    memName,                                                 // 18 이름
    email,                                                   // 19 이메일
    cellPhone,                                               // 20 휴대폰번호
    i % 4 === 0 ? `02-${String(100 + i).slice(-3)}-${String(4000 + i).slice(-4)}` : null, // 21 전화번호
    `0${6000 + (i % 3000)} 서울시 강남구 테헤란로 ${100 + i} ${100 + (i % 900)}호`,        // 22 주소
    i % 2 === 0 ? 'y' : 'n',                                 // 23 메일수신여부
    i % 3 === 0 ? 'y' : 'n',                                 // 24 SMS수신여부
    (i % 7) * 50000,                                         // 25 주문금액
    `${(i % 5) * 300}점`,                                    // 26 마일리지
    '0원',                                                   // 27 예치금
    (i * 13) % 200,                                          // 28 방문횟수
    isBiz ? `(주)${memName}상사` : null,                     // 29 상호
    isBiz ? `${String(100 + i).slice(-3)}-${String(10 + (i % 80)).padStart(2, '0')}-${10000 + i}` : null, // 30 사업자번호
    isBiz ? memName : null,                                  // 31 대표자명
    isBiz ? SERVICES[i % SERVICES.length] : null,            // 32 업태
    isBiz ? ITEMS[i % ITEMS.length] : null,                  // 33 종목
    isBiz ? `0${6000 + (i % 3000)} 서울시 강남구 테헤란로 ${100 + i}` : null, // 34 사업자주소
    null,                                                    // 35 팩스번호
    null,                                                    // 36 추천인아이디
    i % 2 === 0 ? 'm' : 'w',                                 // 37 성별
    `19${85 + (i % 15)}-0${1 + (i % 9)}-1${i % 9}`,          // 38 생일
    null,                                                    // 39 결혼여부
    null,                                                    // 40 결혼기념일
    null,                                                    // 41 직업
    null,                                                    // 42 관심분야
    1,                                                       // 43 개인정보유효기간
    null,                                                    // 44 남기는말씀
    isBiz ? `${ITEMS[(i + 2) % ITEMS.length]}제조` : null,   // 45 추가정보1 (ex1 업종)
    isBiz ? (i % 2 === 0 ? 'Y' : 'N') : null,                // 46 추가정보2 (ex2 세금계산서신청)
    isBiz && i % 3 === 0 ? `${memName} 대리` : null,         // 47 추가정보3 (ex3 담당자)
    null, null, null,                                        // 48~50 추가정보4~6
  ]);
}

const ws = XLSX.utils.aoa_to_sheet(rows);
const wb = XLSX.utils.book_new();
XLSX.utils.book_append_sheet(wb, ws, 'members');
XLSX.writeFile(wb, 'sample-members.xlsx');
console.log(`✅ sample-members.xlsx 생성 완료 — 데이터 ${rows.length - 1}행 (TEMPLATE 51컬럼)`);
