# Iteration 08 Result — 네비게이션 재구성

> 작성일: 2026-06-22
> 담당: Sohee Kang
> 상태: 완료

---

## 완료 요약

단일 페이지(`/`) 구조를 탭 기반 라우팅(`/goods`, `/members`)으로 전환했다.
탭은 52px 헤더 내부에 로고와 아바타 사이에 인라인 배치된다.
`AuthedLayout`이 Header(탭 포함)와 `<Outlet />`을 공통 렌더링하며, 각 탭 페이지는 `<Outlet />`으로 주입된다.

---

## 변경 파일 목록

### 신규 생성

| 파일 | 역할 |
|------|------|
| `src/components/layout/AuthedLayout.tsx` | Header(탭 포함) + Outlet 레이아웃. useLogout, useUserNotifications 호출 담당 |
| `src/pages/GoodsPage.tsx` | DashboardPage 개명. Header 렌더링 제거, 배치 툴바 본문 상단으로 이동 |
| `src/pages/GoodsPage.module.scss` | 툴바·배치 제어 스타일 포함 |
| `src/pages/MembersPage.tsx` | 거래처 등록 관리 플레이스홀더 (Iteration 09에서 구현) |

### 수정

| 파일 | 변경 내용 |
|------|----------|
| `src/App.tsx` | AuthedLayout 중첩 라우팅. `/` → `/goods` 리다이렉트. `/members` 라우트 추가 |
| `src/components/layout/Header.tsx` | 배치 관련 props 5개 제거. 로고 텍스트 변경. 탭 NavLink 인라인 추가 |
| `src/components/layout/Header.module.scss` | 배치 제어 스타일 제거. hdrTabs · tabNav · active 탭 스타일 추가 |
| `src/components/layout/Header.test.tsx` | 제거된 props 반영. 테스트 2번 교체. MemoryRouter 래퍼 추가 |
| `src/pages/LoginPage.tsx` | 로고 텍스트 `판매 상태 자동화 서비스` → `운영 관리` |
| `index.html` | `<title>` `에쓰푸드몰 판매 상태 자동화` → `에쓰푸드몰 운영 관리` |

### 삭제

| 파일 | 이유 |
|------|------|
| `src/pages/DashboardPage.tsx` | GoodsPage.tsx로 교체 |
| `src/pages/DashboardPage.module.scss` | GoodsPage.module.scss로 교체 |

---

## 주요 설계 결정

### 탭을 Header 내부에 인라인 배치

탭 네비게이션은 별도 TabBar 컴포넌트 없이 `Header.tsx` 내부에 NavLink로 직접 렌더링한다.
52px 헤더 요소 안에서 `height: 100%` + `align-items: stretch`를 사용해 탭이 헤더 전체 높이를 채우며 활성 탭의 하단 보더(2px, `--teal`)가 헤더 하단에 맞닿는다.
mockup-members.html의 `.hdr-tabs` / `.tab-nav` 구조를 그대로 따른다.

### 배치 제어 UI 이동

기존 Header 내 배치 제어 UI(상태 동기화 버튼, 자동 실행 팝오버)를 GoodsPage 본문 상단 툴바(`.toolbar`)로 이동했다. Header는 로고 + 탭 + AvatarDropdown만 렌더링하는 전역 레이아웃 역할로 축소된다.

배치 관련 로컬 상태(`intervalInput`, `showIntervalPopover`, `intervalWrapRef`) 및 SVG 아이콘(ClockIcon, PlayIcon, SpinnerIcon)도 Header에서 GoodsPage로 이동했다.

### AuthedLayout의 인증 처리

AuthedLayout은 `getSession` 실패 시 리다이렉트를 수행하지 않는다. 각 라우트의 `RequireAuth`가 인증 실패 처리를 담당하여 역할을 분리했다.

### MemberDetailPage 미생성

iteration-10-plan에서 상세 화면을 페이지가 아닌 모달 오버레이로 변경했으므로 `/members/:memNo` 라우트와 MemberDetailPage 플레이스홀더를 생성하지 않았다.

---

## 검증 결과

| 항목 | 결과 |
|------|------|
| `npm run build` | ✅ 통과 (142 modules, TypeScript 오류 없음) |
| `npm run lint` | ✅ 통과 (경고 0건) |
| `npm run test` | ✅ 통과 (37/37) |

---

## 다음 단계

- Iteration 09: 거래처 등록 관리 목록 페이지 구현 (MembersPage 실 구현)
- Iteration 10: 회원 상세 모달 + 파트너 코드 수정
