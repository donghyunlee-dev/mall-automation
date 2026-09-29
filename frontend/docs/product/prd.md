# PRD — SFOOD-MALL-AUTOMATION-FRONTEND

> Product Requirements Document  
> 에쓰푸드몰 품목 판매 상태 자동화 프론트엔드  
> 참조 문서: architectures.md / api-spec.md  
> 작성일: 2026-04-01

---

## 📋 문서 목적

본 문서는 프론트엔드 개발을 **Iteration 단위**로 분리하여, 각 Iteration이 독립적으로 개발·검증·완료 가능한 상태로 진행되도록 정의한다. Claude Code가 본 문서를 기반으로 순서대로 구현할 수 있도록 각 Iteration 안에 필요한 정보(구현 대상, 컴포넌트 목록, 검증 기준)를 명시한다.

---

## 🧭 Claude Code 문서 정합성 규칙

본 문서는 다음 문서와 함께 해석해야 하며, 구현 시작 전에 모두 읽고 정합성을 확인해야 한다.

- `CLAUDE.md`
- `docs/guide/development-rules.md`
- `docs/guide/testing-strategy.md`
- `docs/guide/done-criteria.md`
- `docs/guide/style-guide.md`
- `docs/guide/api-guide.md`

정합성 원칙은 다음과 같다.

- 사용자 요청이 최우선이다.
- 본 문서의 범위를 초과하여 구현하지 않는다.
- 빌드·린트를 먼저 검증한 뒤 완료로 선언한다.
- 구조 개선이 필요해 보여도 현재 Iteration 범위를 넘으면 구현하지 않는다.
- 완료 기준 충족 후 추가 작업 없이 종료한다.

---

## 🗺️ 전체 Iteration 로드맵

| Iteration | 명칭 | 핵심 목표 | 의존 |
|---|---|---|---|
| Iteration 01 | 프로젝트 기반 설정 | Vite 초기화, 공통 스타일, 라우팅 기반 | 없음 |
| Iteration 02 | 로그인 페이지 | OTP 이메일 입력/확인 UI, 인증 흐름 | Iteration 01 |
| Iteration 03 | 대시보드 레이아웃 | Header, 요약 카드, 기본 레이아웃 | Iteration 02 |
| Iteration 04 | 상품 목록 테이블 | 상품 목록 조회, 검색/필터/정렬/페이지 | Iteration 03 |
| Iteration 05 | 상품 관리 모달 | 상품 추가/수정/삭제, 엑셀 업로드 | Iteration 04 |
| Iteration 06 | 배치 관리 | 수동 실행, 간격 변경, 오늘 변경 이력 | Iteration 04 |
| Iteration 07 | Teams 웹훅 알림 | 배치 실행 후 상태 변경/오류 알림 발송 | Iteration 06 |
| Iteration 08 | 네비게이션 재구성 | TabBar + 라우팅 개편, 프로그램명 변경 | Iteration 07 |
| Iteration 09 | 회원 목록 페이지 | 검색/필터, 서버 페이징, 엑셀 업로드/다운로드 | Iteration 08 / 백엔드 회원 API |
| Iteration 10 | 회원 상세 모달 | 회원 상세 모달 조회, 파트너 코드 수정 | Iteration 09 |

---

## 🔧 공통 개발 원칙

모든 Iteration에서 반드시 준수한다.

**설계 원칙**
- 컴포넌트 — 훅 — API 계층 의존 방향 준수
- 비즈니스 로직은 커스텀 훅 안에만 작성
- 모든 API 호출은 `src/api/` 함수를 통해서만 처리
- `any` 타입 사용 금지
- CSS Modules만 사용, 인라인 스타일 금지

**검증 원칙**
- 구현 후 반드시 `npm run build` + `npm run lint` 실행
- 빌드·린트 오류가 없어야 완료로 인정
- UI는 `docs/design/mockup.html` 기준으로 확인

**산출물 원칙**
- 각 Iteration마다 plan, test-result, result 문서를 작성한다.

---

## 🔧 공통 개발 환경

| 항목 | 내용 |
|---|---|
| 언어/런타임 | TypeScript (strict mode) |
| 프레임워크 | React 18 |
| 빌드 도구 | Vite |
| 스타일 | CSS Modules + SCSS |
| HTTP 클라이언트 | Axios (`withCredentials: true`) |
| 배포 환경 | AWS EC2 + Nginx |

---

## 📋 Iteration 상세

### Iteration 01 — 프로젝트 기반 설정

**목표**: Vite + React + TypeScript 프로젝트 초기화, 공통 스타일 토큰 설정, 라우팅 기반 구성

**구현 대상**

- Vite 프로젝트 초기화 (React + TypeScript strict)
- `src/styles/_variables.scss` — CSS 변수 (디자인 토큰)
- `src/styles/_reset.scss` — 전역 리셋
- `src/styles/global.scss` — @use 모음
- `src/api/client.ts` — Axios 인스턴스, 401 인터셉터
- `App.tsx` — 라우팅 기반 (`/login`, `/`)
- `.env`, `.env.production` — 환경변수

**완료 기준**

- `npm run build` 오류 없음
- `npm run lint` 오류 없음
- CSS 변수가 목업 기준을 따름

---

### Iteration 02 — 로그인 페이지

**목표**: OTP 이메일 입력/확인 UI, 인증 API 연동, 세션 기반 리다이렉트

**구현 대상**

- `src/pages/LoginPage.tsx`
- `src/api/auth.ts` — OTP 발송, 확인 API
- `src/hooks/useAuth.ts` — 로그인 흐름, 세션 확인
- `src/types/auth.ts`
- `LoginPage.module.scss`

**완료 기준**

- UI가 `docs/design/mockup-login.html`과 일치
- OTP 발송 → 확인 → `/` 리다이렉트 흐름 정상 동작
- 401 수신 시 `/login` 리다이렉트 정상 동작
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 03 — 대시보드 레이아웃

**목표**: Header, 현황 요약 카드 3개, 기본 레이아웃 구성

**구현 대상**

- `src/components/layout/Header.tsx`
- `src/components/layout/AvatarDropdown.tsx`
- `src/components/dashboard/SummaryCards.tsx`
- `src/pages/DashboardPage.tsx` (레이아웃 기반)
- 관련 CSS Modules

**완료 기준**

- UI가 `docs/design/mockup.html`의 Header 및 요약 카드와 일치
- 세션 없이 접근 시 `/login` 리다이렉트
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 04 — 상품 목록 테이블

**목표**: 전체 상품 목록 조회, 검색/필터/정렬/페이지네이션

**구현 대상**

- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductSearchBar.tsx`
- `src/components/dashboard/Pagination.tsx`
- `src/api/goods.ts` — 품목 목록 조회 API
- `src/hooks/useGoods.ts` — 목록 조회, 필터, 정렬, 페이지
- `src/types/goods.ts`

**완료 기준**

- UI가 `docs/design/mockup.html`의 상품 목록 테이블과 일치
- 검색/필터/정렬/페이지네이션 정상 동작
- API 오류 상태 처리
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 05 — 상품 관리 모달

**목표**: 상품 추가/수정/삭제, 엑셀 업로드 모달

**구현 대상**

- `src/components/modals/ProductAddModal.tsx`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/ProductDeleteModal.tsx`
- `src/components/modals/ExcelUploadModal.tsx`
- API 함수 추가 (goods.ts — CRUD, 엑셀 업로드)

**완료 기준**

- UI가 `docs/design/mockup.html` 모달 기준과 일치
- 추가/수정/삭제 API 연동 정상 동작
- 엑셀 업로드 정상 동작
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 06 — 배치 관리

**목표**: 수동 실행, 간격 변경, 오늘 변경 이력 테이블

**구현 대상**

- `src/components/dashboard/HistoryTable.tsx`
- `src/components/modals/IntervalConfirmModal.tsx`
- `src/api/batch.ts` — 배치 조회, 수동 실행, 간격 변경
- `src/hooks/useBatch.ts`
- `src/types/batch.ts`

**완료 기준**

- 수동 실행 버튼 정상 동작
- 간격 변경 모달 정상 동작
- 오늘 변경 이력 테이블 정상 표시
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 07 — Teams 웹훅 알림

**목표**: 배치 폴링 기반 판매 상태 변경 감지, Teams Adaptive Card 알림 발송

**구현 대상**

- `src/hooks/useTeamsNotification.ts` — 폴링 기반 배치 결과 감지, Teams 웹훅 발송
- `src/api/teams.ts` — Teams 웹훅 HTTP 요청 함수
- `src/types/teams.ts` — Adaptive Card 메시지 타입 정의
- 알림 발송 조건
  - 배치 실행 후 상태 변경 이력이 1건 이상인 경우: 상태 변경 알림 카드 발송
  - 배치 실행 중 오류 발생 시: 오류 알림 카드 발송

**알림 카드 포맷**

- 상태 변경 알림: 실행 시각, 변경 건수, 품목별 변경 내역 테이블, 대시보드 바로가기 링크 포함
- 오류 알림: 발생 시각, 오류 내용, "담당 개발자에게 문의해주세요" 안내 문구 포함 (대시보드 링크 없음)
- 카드 형식: Adaptive Card v1.4 (`application/vnd.microsoft.card.adaptive`)

**완료 기준**

- 배치 실행 후 변경 이력이 있을 때 Teams 채널에 상태 변경 카드 발송
- 배치 오류 발생 시 Teams 채널에 오류 알림 카드 발송
- 변경 이력이 없을 때는 알림 미발송
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 08 — 네비게이션 재구성

**목표**: 단일 페이지(`/`) 구조를 탭 기반 멀티 페이지(`/goods`, `/members`)로 개편하고, 거래처 등록 관리 추가에 맞춰 프로그램명을 변경한다. 기존 상품 관리 기능은 영향받지 않는다.

**구현 대상**

- `App.tsx` — 라우트 재구성(`/goods`, `/members`, `/members/:memNo`) + `AuthedLayout` 도입
- `src/components/layout/TabBar.tsx` — 상단 탭 네비게이션(상품 관리 / 거래처 등록 관리)
- `src/components/layout/AuthedLayout.tsx` — Header + TabBar + `<Outlet />`
- `src/pages/DashboardPage.tsx` → `src/pages/GoodsPage.tsx` 개명(내부 로직 변경 없음)
- 헤더 전역화 — 상태 동기화·자동 실행 시간 설정 컨트롤을 상품 관리 탭 본문 상단으로 이동
- 프로그램명 변경 — `에쓰푸드몰 판매 상태 자동화` → `에쓰푸드몰 운영 관리` (헤더 로고·로그인 화면·`index.html` title)
- `src/pages/MembersPage.tsx`, `src/pages/MemberDetailPage.tsx` — 플레이스홀더(Iter 09·10에서 구현)

**완료 기준**

- `/` 접근 시 `/goods` 리다이렉트, 탭 전환 정상 동작
- 기존 상품 관리 기능(검색·필터·업로드·배치) 정상 동작
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 09 — 회원 목록 페이지

**목표**: 회원 목록 페이지(`/members`) 구현 — 검색/필터, 서버 사이드 페이징(2.3만 건 대응), 엑셀 업로드·다운로드

**구현 대상**

- `src/types/member.ts` — 회원 도메인 타입
- `src/api/members.ts` — 목록·업로드 API 함수
- `src/hooks/useMembers.ts` — 목록 상태·페이징·URL 동기화
- `src/hooks/useMemberUpload.ts` — 엑셀 파싱 → 업로드
- `src/utils/memberGroup.ts`, `src/utils/parseMemberExcel.ts`
- `src/components/members/MemberSearchBar.tsx`, `MemberTable.tsx`
- `src/components/modals/MemberUploadModal.tsx`
- `src/pages/MembersPage.tsx` (플레이스홀더 대체)

**완료 기준**

- 서버 페이징 테이블 로드 (기본 10개씩 보기)
- 등급 필터(사업자/B2B), 검색 타입(아이디/이름/거래처코드/거래처명) 동작
- 페이지 크기 드롭다운 동작
- 엑셀 업로드(단일 호출 `POST /api/members/uploads`) · 엑셀 다운로드 정상 동작
- `npm run build` + `npm run lint` 오류 없음

---

### Iteration 10 — 회원 상세 모달

**목표**: 회원 상세 모달(`MemberDetailModal`) 구현 — 행 클릭 시 모달 오버레이, 복호화 평문 표시, 조건부 섹션, 파트너 코드 인라인 수정

**구현 대상**

- `src/hooks/useMemberDetail.ts` — 상세 조회 + 파트너 수정
- `src/components/members/MemberDetailModal.tsx` — 상세 모달 (파트너 수정 인라인 폼 포함)
- `src/types/member.ts`·`src/api/members.ts` — 상세·파트너 수정 타입·함수 추가

**완료 기준**

- 행 클릭 → 상세 모달 오픈, 복호화 평문 표시(이름·이메일·휴대폰 마스킹 없음)
- `group_sno`별 조건부 섹션(기본/사업자/에쓰푸드 거래처/추가) 정상 표시·숨김
- 에쓰푸드 거래처 정보 카드 인라인 편집 → 저장 → 즉시 반영
- 모달 닫기(× 또는 배경 클릭) 시 목록 검색 조건 유지
- `npm run build` + `npm run lint` 오류 없음

---

## 📄 산출물 구조

각 Iteration마다 다음 문서를 작성한다.

```text
docs/plan/iteration/iteration-XX-plan.md    ← 구현 시작 전 작성
docs/test/results/iteration-XX-test-result.md ← 검증 결과 기록
docs/reports/iteration-XX-result.md         ← 완료 후 요약
```

Iteration 번호는 두 자리로 표기한다 (01, 02, 03...).
