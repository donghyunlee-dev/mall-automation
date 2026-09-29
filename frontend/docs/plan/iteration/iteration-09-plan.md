# Iteration 09 Plan — 회원 목록 페이지

> 작성일: 2026-06-11
> 담당: Sohee Kang
> 상태: 대기 (백엔드 Iteration 35~37 완료 후 착수)
> 의존: Iteration 08 (네비게이션), 백엔드 Iteration 37 (목록·요약·업로드 API)
> 이후: Iteration 10 (회원 상세 페이지)
> 참조: `backend/docs/plan/member-management-ui-plan.md` 4-1·8·9-2·9-3, `backend/docs/product/api-spec-member.md`

---

## 목표

회원 목록 페이지(`/members`)를 구현한다. 핵심 기능은 다음과 같다.

- 검색 타입 선택 + 키워드 입력
- 등급 필터 (사업자 / B2B)
- 서버 사이드 페이징 테이블 (23,000건 대응)
- 페이지 크기 선택 드롭다운
- 엑셀 업로드·다운로드

---

## 사전 확인 사항

백엔드 API 착수 전에 다음을 확인한다.

| 항목 | 확인 방법 |
|------|----------|
| `GET /api/members` 응답 스키마 | `api-spec-member.md` 참조 |
| `POST /api/members/uploads` 단일 업로드 (`type`/`fileName`/`applyDeletion`/`rows`) | `api-spec-member.md` 참조 |
| 정렬 허용 컬럼 목록 | `mem_no, mem_id, group_sno, app_fl, entry_dt, last_login_dt, sale_amount` |
| 검색 타입별 동작 | `MEM_ID`: 부분검색 / `NAME·EMAIL·CELL_PHONE·PARTNER_CODE·PARTNER_NAME`: 정확일치 |
| searchType 추가 예정 | `PARTNER_CODE`·`PARTNER_NAME`은 백엔드 추가 예정 — 착수 전 `api-spec-member.md` 업데이트 확인 |

---

## 범위

### 신규 파일

| 파일 | 역할 |
|------|------|
| `src/types/member.ts` | 회원 도메인 타입 전체 |
| `src/api/members.ts` | 목록·업로드 API 함수 |
| `src/hooks/useMembers.ts` | 목록 상태·페이징·URL 동기화 |
| `src/hooks/useMemberUpload.ts` | SheetJS 파싱 → 청크 전송 루프 |
| `src/utils/memberGroup.ts` | group_sno → 등급명·배지 색상 매핑 |
| `src/utils/parseMemberExcel.ts` | SheetJS 파싱 + 파일 종류 판별 |
| `src/components/members/MemberSearchBar.tsx` | 검색 타입 + 키워드 + 필터 + 페이지 크기 |
| `src/components/members/MemberSearchBar.module.scss` | |
| `src/components/members/MemberTable.tsx` | 회원 목록 테이블 |
| `src/components/members/MemberTable.module.scss` | |
| `src/components/modals/MemberUploadModal.tsx` | 엑셀 업로드 모달 |
| `src/pages/MembersPage.tsx` | 목록 페이지 (Iteration 08 플레이스홀더 대체) |
| `src/pages/MembersPage.module.scss` | |

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/pages/MembersPage.tsx` | 플레이스홀더 → 실 구현으로 교체 |

---

## 타입 명세 (`src/types/member.ts`)

```typescript
// 목록 행 (마스킹 응답)
export interface MemberListItem {
  memNo: number;
  memId: string;
  memName: string | null;     // 마스킹 ("홍*동")
  groupSno: number;
  groupName: string;
  email: string | null;       // 마스킹 ("ho***@example.com")
  company: string | null;
  businessNo: string | null;  // 사업자 등급 외 null
  ceoName: string | null;     // 복호화 평문, 사업자 등급 외 null
  service: string | null;     // 업태, 사업자 등급 외 null
  item: string | null;        // 종목, 사업자 등급 외 null
  comAddress: string | null;  // 사업자 주소(복호화 평문), 사업자 등급 외 null
  partnerCode: string | null;
  partnerName: string | null; // member_partner 없으면 null
  entryDt: string;
  appFl: 'y' | 'n';
}

// 페이징 래퍼
export interface MemberPage {
  content: MemberListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// 검색 조건 (URL 쿼리 파라미터와 동기화)
// PARTNER_CODE·PARTNER_NAME은 백엔드 추가 예정 — 착수 전 api-spec-member.md 업데이트 확인
export interface MemberSearchParams {
  searchType: 'MEM_ID' | 'NAME' | 'PARTNER_CODE' | 'PARTNER_NAME' | 'EMAIL' | 'CELL_PHONE';
  keyword: string;
  group: 'ALL' | 'BUSINESS' | 'B2B';
  sortKey: string;
  sortDir: 'asc' | 'desc';
  page: number;
  size: number;
  entryDtFrom: string;  // '' = 미지정
  entryDtTo: string;    // '' = 미지정
}

// 업로드 (단일 원자적 호출 — POST /api/members/uploads)
export type MemberUploadType = 'LIST' | 'TEMPLATE';

export interface MemberUploadRequest {
  type: MemberUploadType;
  fileName: string;
  applyDeletion: boolean;
  rows: (string | number | null)[][];  // 헤더 제외 원본 셀 값
}

export interface MemberUploadResponse {
  totalInserted: number;
  totalUpdated: number;
  totalDeleted: number;
  totalFailed: number;
  failedRows: Array<{ rowIndex: number; reason: string }>;
  deletionSkipped: boolean;
  deletionSkippedReason: string | null;
  syncedAt: string;
}
```

---

## API 함수 명세 (`src/api/members.ts`)

```typescript
import client from './client';
import type {
  MemberPage, MemberSearchParams,
  MemberUploadRequest, MemberUploadResponse
} from '../types/member';

// 목록 조회 — 빈 배열 기본값 보장
export async function getMembers(params: Partial<MemberSearchParams>): Promise<MemberPage> {
  const res = await client.get('/api/members', { params });
  return {
    content: res.data.content ?? [],
    page: res.data.page ?? 0,
    size: res.data.size ?? 10,
    totalElements: res.data.totalElements ?? 0,
    totalPages: res.data.totalPages ?? 0,
  };
}

// 회원 일괄 업로드 (단일 원자적 호출)
export async function uploadMembers(
  req: MemberUploadRequest
): Promise<MemberUploadResponse> {
  const res = await client.post('/api/members/uploads', req);
  return res.data;
}
```

---

## 훅 명세

### useMembers.ts

**역할:** 목록 상태 관리, URL 쿼리 파라미터 동기화, 검색 조건 변경 시 API 재호출

```typescript
interface UseMembersReturn {
  members: MemberPage | null;
  loading: boolean;
  error: string | null;
  searchParams: MemberSearchParams;
  setSearchParams: (params: Partial<MemberSearchParams>) => void;
  resetSearchParams: () => void;
}
```

**설계 방침:**
- `useSearchParams` (React Router)로 URL 쿼리 파라미터와 동기화
  - 검색 조건 변경 → URL 업데이트 → API 호출
  - 상세 진입 후 뒤로가기 → URL에서 조건 복원 → 동일 페이지·필터 유지
- 키워드는 Enter/검색 버튼 제출 방식 (debounce 미적용 — 해시 정확일치 특성)
- 페이지 변경 시 스크롤 상단 이동

**URL 파라미터 매핑:**

| 상태 | URL 파라미터 | 기본값 |
|------|------------|--------|
| searchType | st | MEM_ID |
| keyword | kw | (빈 문자열) |
| group | gr | ALL |
| sortKey | sk | mem_no |
| sortDir | sd | desc |
| page | pg | 1 |
| size | sz | 10 |
| entryDtFrom | df | (빈 문자열 = 전체) |
| entryDtTo | dt | (빈 문자열 = 전체) |

### useMemberUpload.ts

**역할:** SheetJS 파싱 → 단일 업로드 호출 → 결과 처리

```typescript
interface UseMemberUploadReturn {
  status: 'idle' | 'parsing' | 'uploading' | 'done' | 'error';
  detectedType: MemberUploadType | null;    // 파일 자동 판별 결과
  result: MemberUploadResponse | null;
  error: string | null;
  isDamagedFile: boolean;
  upload: (file: File, applyDeletion: boolean) => Promise<void>;
  reset: () => void;
}
```

**업로드 흐름:**
1. `parseMemberExcel(file)` → `{ type, rows, isDamaged }`
2. `isDamaged` → `status = 'error'`, 손상 파일 메시지 표시 후 중단
3. `status = 'uploading'` → `uploadMembers({ type, fileName, applyDeletion, rows })` 단일 호출
4. `status = 'done'`, result 저장 (실패 행은 `result.failedRows`로 표시)

### parseMemberExcel.ts

SheetJS(`xlsx` — 기존 의존성)로 파싱:

```typescript
interface ParseResult {
  type: MemberUploadType;
  rows: (string | number | null)[][];  // 헤더 제외, 원본 셀 값
  isDamaged: boolean;  // 파싱 후 0행이면 true
}

export function parseMemberExcel(file: File): Promise<ParseResult>
```

**파일 종류 자동 판별:**
- 헤더 행 첫 번째 컬럼 값이 "회원번호" → `LIST`
- 첫 번째 컬럼 값이 "아이디" → `TEMPLATE`
- 판별 실패 → `LIST`로 기본 처리 + 콘솔 경고

**손상 파일 감지:**
- SheetJS 파싱 후 rows.length === 0 → `isDamaged = true`

### memberGroup.ts

```typescript
interface MemberGroupInfo {
  label: string;
  badgeBg: string;   // CSS 변수명
  badgeText: string; // CSS 변수명
}

const GROUP_MAP: Record<number, MemberGroupInfo> = {
  1: { label: '일반회원', badgeBg: 'var(--surface3)', badgeText: 'var(--text2)' },
  2: { label: '사업자회원_화이트', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  3: { label: '사업자회원_로제', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  5: { label: '사업자회원_레드', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  6: { label: '사업자회원_VIP', badgeBg: 'var(--teal-bg)', badgeText: 'var(--teal)' },
  7: { label: '관리자', badgeBg: '#fef3c7', badgeText: '#92400e' },
  8: { label: '기능장회원', badgeBg: 'var(--surface3)', badgeText: 'var(--text2)' },
  9: { label: 'B2B 거래처', badgeBg: 'var(--purple-bg)', badgeText: 'var(--purple)' },
};

export function getMemberGroupInfo(groupSno: number): MemberGroupInfo
export function isBusiness(groupSno: number): boolean  // 2,3,5,6,9
```

---

## 컴포넌트 명세

### MemberSearchBar.tsx

```
Props: { searchParams: MemberSearchParams; onSearch: (params: Partial<MemberSearchParams>) => void }
```

두 행 레이아웃 (`mockup-members.html` 기준):

**Row 1 — 가입일**
- `가입일` 라벨
- 날짜 from 입력 (`type="date"`, 기본값: 비어 있음 = 전체)
- `~`
- 날짜 to 입력 (`type="date"`, 기본값: 비어 있음 = 전체)
- 빠른 선택 버튼: 전체 | 오늘 | 일주일 | 1개월 | 이번달 | 지난달 (선택 시 즉시 조회, 활성 상태 하이라이트)
- 초기 진입 시 `전체` 프리셋 활성 상태로 표시 (활성 상태는 날짜값에서 파생)

**Row 2 — 등급 + 검색**
- `등급` 라벨
- 등급 드롭다운: ALL(전체) | BUSINESS(사업자) | B2B
- 검색 타입 드롭다운: MEM_ID(아이디) | NAME(이름) | PARTNER_CODE(거래처코드) | PARTNER_NAME(거래처명) | EMAIL(이메일) | CELL_PHONE(휴대폰)
  - MEM_ID 외 선택 시 keyword placeholder: "정확히 일치하는 값 입력"
- 키워드 입력 + 지우기(X) 버튼
- 조회 버튼 (Enter 키도 제출)
- 우측 끝 (`margin-left: auto`): 페이지 크기 드롭다운 (기본 10개씩 보기, 10 | 20 | 30 | 50)

### MemberTable.tsx

```
Props: {
  members: MemberListItem[];
  sortKey: string;
  sortDir: 'asc' | 'desc';
  onSort: (key: string) => void;
  onRowClick: (memNo: number) => void;
}
```

- 정렬 허용 컬럼: `mem_no, mem_id, group_sno, app_fl, entry_dt, last_login_dt, sale_amount`
  - 해당 컬럼만 `SortableTh` 적용 (기존 `ProductTable`의 `SortableTh` 패턴 재사용)
  - `memName`, `email` 컬럼(암호화·마스킹) — 정렬 아이콘 미노출
- 행 전체 클릭 → `onRowClick` (상세 이동)
  - '고도몰' 링크 아이콘은 `stopPropagation` 처리
- 등급 배지: `memberGroup.ts`의 `getMemberGroupInfo()` 사용
- 빈 값 표시: `—`
- 로딩 상태: 테이블 영역 흐림 처리 (spinner overlay)
- 빈 상태 (`.tableWrap` 내 `position: absolute` 오버레이로 정중앙 배치):
  - 데이터 없음: "아직 등록된 회원이 없습니다" + "우측 상단의 엑셀 업로드 버튼을 눌러 회원을 등록해 주세요."
  - 검색 결과 없음: "검색 결과가 없습니다" + 초기화 버튼

### MemberUploadModal.tsx

```
Props: { open: boolean; onClose: () => void; onSuccess: () => void }
```

**모달 단계별 UI:**

1. **파일 선택**: 드래그앤드롭 + 파일 선택 버튼 (`.xls`, `.xlsx` 허용)
2. **파일 감지됨**: 파일명, 자동 감지 결과 ("고도몰 회원 리스트 파일로 인식됨" / "분석 템플릿 파일로 인식됨"), 손상 파일 안내 (해당 시)
3. **삭제 옵션**: "이 파일은 전체 회원 목록입니다 (목록에 없는 회원을 탈퇴 처리)" 체크박스 — 기본 해제
4. **업로드 중**: 로딩 인디케이터("회원 데이터를 처리하고 있어요"), 닫기 버튼 비활성 — 단일 호출이라 처리 완료까지 대기
5. **완료**: 신규 N / 갱신 N / 탈퇴 처리 N / 실패 N 요약, 실패 행 펼쳐보기, 닫기 버튼

손상 파일 안내 문구:
> "Excel에서 다시 저장한 파일은 데이터가 빠질 수 있습니다. 고도몰에서 받은 원본 파일을 그대로 업로드해 주세요."

deletionSkipped 시 경고 표시:
> "삭제 대상이 전체 회원의 10%를 초과하여 탈퇴 처리가 보류되었습니다. 전체 파일인지 확인 후 재시도해 주세요."

---

## MembersPage.tsx 레이아웃

```
┌────────────────────────────────────────────────────────────────┐
│ 회원 목록  23,156명   마지막 업로드: YYYY-MM-DD HH:MM  [엑셀 업로드] [엑셀 다운로드] │
├────────────────────────────────────────────────────────────────┤
│ 가입일  [날짜 from] ~ [날짜 to]  [전체] [오늘] [일주일] [1개월] [이번달] [지난달]    │
├────────────────────────────────────────────────────────────────┤
│ 등급  등급 ▼  ┃  검색유형 ▼  [🔍 키워드 입력  ×]  [조회]  (auto) 10개씩 보기 ▼    │
├────────────────────────────────────────────────────────────────┤
│ [회원 목록 테이블]                                              │
│ [페이지네이션]                                                  │
└────────────────────────────────────────────────────────────────┘
```

재사용 컴포넌트: `Pagination.tsx` (기존), `Toast.tsx` (기존)

---

## 테스트 명세

### memberGroup.test.ts

```typescript
// group_sno 1~9 매핑 전수 확인
// isBusiness(2, 3, 5, 6, 9) === true
// isBusiness(1, 7, 8) === false
```

### members.test.ts

```typescript
// getMembers: content 빈 배열 기본값, totalElements 기본값 0
// uploadMembers: 단일 POST /api/members/uploads 호출, 요청 바디(type/fileName/applyDeletion/rows) 확인
// axios mock 사용
```

### useMembers.test.tsx

```typescript
// 조건 변경 시 API 재호출 파라미터 확인
// URL 쿼리 파라미터 동기화 확인 (useSearchParams mock)
// 로딩 상태 전환 확인
```

### MemberTable.test.tsx

```typescript
// 마스킹 값 표시 (이름 "홍*동", 이메일 "ho***@example.com")
// group_sno 별 배지 표시
// 정렬 불가 컬럼(memName, email)에 정렬 UI 미노출
// 행 클릭 → onRowClick 호출, 고도몰 링크 클릭 → stopPropagation
// 빈 상태 메시지 표시
```

### parseMemberExcel.test.ts (단순 케이스 — 실제 파일 없이 mock 데이터)

```typescript
// 헤더 "회원번호" → type = LIST
// 헤더 "아이디" → type = TEMPLATE
// rows.length === 0 → isDamaged = true
```

---

## 완료 기준

- `npm run build` — TypeScript 오류 없음
- `npm run lint` — 오류 없음
- `npm run test` — 위 테스트 전체 통과
- 수동 확인:
  - `/members` 진입 → 테이블 로드 (기본 10개씩 보기)
  - 등급 드롭다운 변경 → 필터 즉시 적용
  - 검색 타입 변경 → placeholder 변경
  - 키워드 입력 + Enter → 검색 파라미터 URL 반영, API 재호출
  - 페이지 크기 드롭다운 변경 → size 파라미터 적용
  - 페이지 전환 → URL `pg` 파라미터 변경
  - 상세 진입 후 뒤로가기 → 동일 검색 조건·페이지 복원
  - 엑셀 업로드 모달 → 진행률 표시, 완료 후 목록 재조회
  - 23,000행 실파일 업로드 후 서버 페이징 체감 성능 확인
