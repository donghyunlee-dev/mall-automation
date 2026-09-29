# Iteration 10 Plan — 회원 상세 페이지 + 파트너 코드 수정

> 작성일: 2026-06-11
> 담당: Sohee Kang
> 상태: 대기 (백엔드 Iteration 37·38 완료 후 착수)
> 의존: Iteration 09 (회원 목록), 백엔드 Iteration 38 (상세·파트너 수정 API)
> 참조: `backend/docs/plan/member-management-ui-plan.md` 4-2·9-4, `backend/docs/product/api-spec-member.md`

---

## 목표

회원 상세 모달(`MemberDetailModal`)을 구현한다. 회원 목록에서 행 클릭 시 모달 오버레이로 상세 정보를 표시한다. 핵심 기능은 다음과 같다.

- 복호화 평문 표시 (이름·이메일·휴대폰·주소 — 마스킹 없음)
- 기본 정보 / 사업자 정보 / 에쓰푸드 거래처 정보 조건부 섹션
- 파트너 코드 수정 모달
- 고도몰 어드민 이동 링크

---

## 사전 확인 사항

| 항목 | 확인 방법 |
|------|----------|
| `GET /api/members/{memNo}` 응답 스키마 | `api-spec-member.md` 참조 |
| `PUT /api/members/{memNo}/partner` 요청·응답 | `api-spec-member.md` 참조 |
| 조건부 섹션 기준 | `hasBusiness`: group_sno IN (2,3,5,6,9) / `isB2B`: group_sno === 9 |
| 고도몰 어드민 링크 형식 | 백엔드 팀에 URL 패턴 확인 후 constants.ts에 정의 |

---

## 범위

### 신규 파일

| 파일 | 역할 |
|------|------|
| `src/types/member.ts` 추가 | 상세·파트너 수정 타입 추가 (기존 파일 수정) |
| `src/api/members.ts` 추가 | 상세 조회·파트너 수정 API 함수 추가 (기존 파일 수정) |
| `src/hooks/useMemberDetail.ts` | 상세 조회 + 파트너 수정 훅 |
| `src/components/members/MemberDetailModal.tsx` | 상세 모달 (상세 정보 + 파트너 코드 인라인 수정 포함) |
| `src/components/members/MemberDetailModal.module.scss` | |

### 구현 중 추가된 범위 (사용자 요청)

상세 모달 외에 목록 사용성 보강이 함께 진행되었다. 상세 내역은 `docs/reports/iteration-10-result.md` 참조.

- 목록 좌측 체크박스(전체선택 + 행별) + 선택 회원 엑셀 다운로드 — 분석 템플릿(51컬럼) 형식에 상세 데이터 병합, 클라이언트 생성 (`src/utils/exportMembersExcel.ts`)
- 휴대폰(cellPhone) 목록 컬럼 복원 (목업 기준, 백엔드 재추가 요청)
- 가입일 필터 `전체` 프리셋 추가·기본 활성, 활성 상태는 날짜값에서 파생
- 업로드 완료 시 새로고침 없이 목록 즉시 반영 (`useMembers.reload()`)
- 기본 정보 "읽기 전용" 안내문구 제거

---

## 타입 추가 (`src/types/member.ts`)

```typescript
// 추가 타입 (기존 파일에 append)

export interface MemberBusinessDetail {
  company: string | null;
  businessNo: string | null;     // 평문
  ceoName: string | null;        // 복호화 평문
  service: string | null;
  item: string | null;
  comAddress: string | null;     // 복호화 평문
  bizLicenseFile: string | null;
}

export interface MemberPartnerDetail {
  partnerRegisteredFl: 'y' | 'n';
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n';
  duplicateCheck: string | null;
  updatedAt: string;
}

export interface MemberDetail {
  memNo: number;
  memId: string;
  memName: string | null;        // 복호화 평문 (마스킹 없음)
  groupSno: number;
  groupName: string;
  appFl: 'y' | 'n';
  appDt: string | null;
  email: string | null;          // 복호화 평문
  cellPhone: string | null;      // 복호화 평문
  phone: string | null;          // 복호화 평문
  address: string | null;        // 복호화 평문
  entryDt: string;
  lastLoginDt: string | null;
  entryPath: string | null;
  maillingFl: 'y' | 'n';
  smsFl: 'y' | 'n';
  nickName: string | null;
  sexFl: 'm' | 'w' | null;
  birthDt: string | null;        // 복호화 평문
  marriFl: 'y' | 'n' | null;
  marriDate: string | null;
  job: string | null;
  interest: string | null;
  loginCount: number;
  saleAmount: number;   // 현재 모달 UI에 미표시 (향후 확장 시 사용)
  mileage: number;      // 현재 모달 UI에 미표시
  deposit: number;      // 현재 모달 UI에 미표시
  recommId: string | null;
  ex1: string | null;
  ex2: string | null;
  ex3: string | null;
  ex4: string | null;
  ex5: string | null;
  ex6: string | null;
  memo: string | null;
  business: MemberBusinessDetail | null;  // hasBusiness 그룹만
  partner: MemberPartnerDetail | null;    // 등록된 경우만
}

export interface UpdatePartnerRequest {
  partnerRegisteredFl: 'y' | 'n' | null;  // null 전달 시 서버에서 'n' 처리
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n' | null;         // null 전달 시 서버에서 'n' 처리
  duplicateCheck: string | null;
  bizLicenseFile: string | null;          // 사업자 레코드 없는 회원은 무시됨
}

export interface MemberPartnerResponse {
  partnerRegisteredFl: 'y' | 'n';
  partnerCode: string | null;
  partnerName: string | null;
  taxInvoiceFl: 'y' | 'n';
  duplicateCheck: string | null;
  bizLicenseFile: string | null;
  updatedAt: string;
}
```

---

## API 함수 추가 (`src/api/members.ts`)

```typescript
// 기존 파일에 추가

export async function getMemberDetail(memNo: number): Promise<MemberDetail> {
  const res = await client.get(`/api/members/${memNo}`);
  return res.data;
}

export async function updateMemberPartner(
  memNo: number,
  req: UpdatePartnerRequest
): Promise<MemberPartnerResponse> {
  const res = await client.put(`/api/members/${memNo}/partner`, req);
  return res.data;
}
```

---

## 훅 명세 (`useMemberDetail.ts`)

```typescript
interface UseMemberDetailReturn {
  member: MemberDetail | null;
  loading: boolean;
  error: string | null;
  updatePartner: (req: UpdatePartnerRequest) => Promise<void>;
  partnerUpdating: boolean;
  refetch: () => void;
}
```

- 마운트 시 `getMemberDetail(memNo)` 호출
- `updatePartner` 완료 후 `refetch()` 자동 호출 → 최신 데이터 반영
- 404 오류 → 목록으로 리다이렉트 + Toast 오류 메시지

---

## 컴포넌트 명세

### MemberDetailModal.tsx 레이아웃 (mockup-members.html 기준)

```
┌─────────────────────────────────────────────────────────────────────┐
│ [아이디] [등급 배지]            [고도몰 어드민 열기 ↗] [×]          │
├─────────────────────────────────────────────────────────────────────┤
│  기본 정보                                                          │
│  회원번호: 12345        아이디: hong123                            │
│  이름: 홍길동           휴대폰: 010-1234-5678                      │
│  이메일: hong@…         전화: 02-123-4567                          │
│  주소: 06000 서울시 강남구 …      (2열 grid, cols2)                │
│  가입일 / 경로: 2025-01-15 / pc   최근 접속: 2026-06-01            │
│  ※ 기본 정보는 읽기 전용 — 고도몰 엑셀 재업로드로만 갱신됩니다.     │
├─────────────────────────────────────────────────────────────────────┤
│  사업자 정보 (isBusiness && business≠null)        (info-body split) │
│  ┌── biz-main ──────────────┐  ┌── 추가 정보 (biz-extra) ──────┐  │
│  │ 상호: (주)테스트           │  │ 업종: 식품제조 (ex1)          │  │
│  │ 사업자번호: 123-45-67890   │  │ 세금계산서 신청: 발행필요(ex2)│  │
│  │ 대표자명: 홍길동           │  │ 담당자: — (ex3)               │  │
│  │ 업태 / 종목: 도매 / 식품   │  └───────────────────────────────┘  │
│  │ 사업장 주소: 06000 …       │                                     │
│  │ 사업자등록증: license.pdf  │                                     │
│  └────────────────────────────┘                                     │
├─────────────────────────────────────────────────────────────────────┤
│  에쓰푸드 거래처 정보 (groupSno=9 또는 partner≠null)       [저장]   │
│  거래처 등록: [등록됨] [미등록]                                     │
│  거래처 코드: __________                                            │
│  거래처명: __________                                               │
│  세금계산서: [발행(Y)] [미발행(N)]                                 │
│  최종 수정: 2026-06-11 14:30 (읽기 전용)                           │
└─────────────────────────────────────────────────────────────────────┘
```

조건부 카드(사업자/거래처)는 mockup의 `info-grid`를 단일 열(`1fr`)로 세로 적층한다.

**섹션 표시 조건:**
- 사업자 정보: `isBusiness(member.groupSno) && member.business !== null`
  - 내부 "추가 정보(biz-extra)"는 사업자 정보 카드의 우측 서브섹션으로 통합 — ex1(업종)·ex2(세금계산서 신청)·ex3(담당자) 표시 (값 없으면 `—`)
- 에쓰푸드 거래처 정보: `member.groupSno === 9 || member.partner !== null`
  - mockup 규칙은 group_sno=9이나, 도메인상 거래처(member_partner)는 사업자·기능장에도 존재할 수 있어 partner 레코드 존재 시에도 노출 (데이터 은닉 방지)

**모달 동작:**
- 배경(오버레이) 클릭 또는 `×` 버튼 → 모달 닫기 (목록 검색 조건 유지)
- 최대 너비: 860px, 최대 높이: 90vh (스크롤 가능)

**고도몰 어드민 링크:**
- 실 URL 패턴은 백엔드 확인 필요 — `src/utils/constants.ts`에 `GODO_ADMIN_MEMBER_URL` 플레이스홀더 정의 (확정 시 교체)
- `window.open(`${GODO_ADMIN_MEMBER_URL}${memNo}`, '_blank', 'noopener,noreferrer')`

**사업자등록증 파일:**
- 다운로드 엔드포인트 미정 — 파일명만 텍스트 표시 (실 링크 미연동)

**에쓰푸드 거래처 정보 (인라인 편집):**

별도 버튼 없이 카드 자체가 항상 편집 가능 상태. 카드 헤더 우측에 `저장` 버튼.

폼 필드:
- 거래처 등록 여부 (toggle: 등록됨 / 미등록) — partnerRegisteredFl
- 거래처 코드 (text input) — 등록됨 선택 시 필수, partnerCode
- 거래처명 (text input) — partnerName
- 세금계산서 발행 (toggle: 발행(Y) / 미발행(N)) — taxInvoiceFl
- 최종 수정 (읽기 전용 — 서버 응답 updatedAt)

검증: `partnerRegisteredFl = "y"` && `partnerCode` 비어있음 → "거래처 코드를 입력해 주세요"

저장 후: `useMemberDetail.updatePartner` 완료 → 상세 데이터 자동 갱신 + Toast 성공 메시지
파트너 레코드 없는 회원(LIST 업로드) 저장 시 404(`MEMBER_PARTNER_NOT_FOUND`) → "템플릿 엑셀 업로드로 거래처를 먼저 등록해 주세요" Toast

> **갱신 안내(2026-07-10)**: 이 계획은 파트너 레코드가 없으면 백엔드가 `404 MEMBER_PARTNER_NOT_FOUND`를 반환한다고 가정했다. 이후 `UpdateMemberPartnerUseCase`가 파트너 레코드 없을 때 INSERT하도록 변경되어(`547f1ea`, 2026-07-03) 이 404 케이스는 더 이상 발생하지 않는다 — 현재 404는 `memNo` 자체가 없을 때(`MEMBER_NOT_FOUND`)만 발생한다. `src/hooks/useMemberDetail.ts`의 404 Toast 문구는 이 변경 이후 갱신되지 않은 상태로 확인됨(수정 필요, 별도 확인 진행 중).

---

## 테스트 명세

### useMemberDetail.test.tsx

```typescript
// getMemberDetail 호출 → member 상태 설정
// updatePartner 호출 후 refetch 자동 실행
// 404 응답 → 목록 리다이렉트
// loading 상태 전환
```

### MemberDetailModal.test.tsx

```typescript
// hasBusiness 그룹 → 사업자 정보 섹션 표시 (추가 정보 ex1~ex3 포함)
// 비사업자 그룹(일반, business=null) → 사업자 정보 섹션 미표시
// group_sno=9 → 에쓰푸드 거래처 정보 섹션 표시
// group_sno≠9 && partner=null → 에쓰푸드 거래처 정보 섹션 미표시
// 거래처 코드 미입력 + 등록됨 저장 → 검증 오류 (updatePartner 미호출)
// 배경 클릭 또는 × 버튼 → onClose 호출
```

---

## 완료 기준

- `npm run build` — TypeScript 오류 없음
- `npm run lint` — 오류 없음
- `npm run test` — 위 테스트 전체 통과
- 수동 확인:
  - 회원 목록 → 행 클릭 → 상세 모달 오픈
  - 상세 모달 복호화 평문 표시 (이름·이메일·휴대폰 마스킹 없음)
  - group_sno 별 조건부 섹션 정상 표시·숨김
  - 에쓰푸드 거래처 정보 카드 편집 후 저장 → 즉시 반영 확인
  - × 버튼 또는 배경 클릭 → 모달 닫힘, 목록 검색 조건 유지
  - 고도몰 어드민 링크 → 새 탭 오픈

---

## 거래처 등록 관리 기능 전체 완료 기준 (Iteration 08~10)

| Iteration | 주요 기능 | 완료 기준 |
|-----------|----------|----------|
| 08 | 네비게이션 재구성 | 탭 전환, 라우팅, 빌드·린트 통과 |
| 09 | 회원 목록 + 업로드 | 서버 페이징, 검색, 실파일 업로드 |
| 10 | 회원 상세 모달 + 파트너 수정 | 모달 오픈, 평문 표시, 조건부 섹션, 인라인 수정 반영 |

세 Iteration 완료 후 `backend/docs/plan/member-management-ui-plan.md` Phase 1·2 완료 상태.
