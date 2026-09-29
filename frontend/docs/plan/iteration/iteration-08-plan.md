# Iteration 08 Plan — 네비게이션 재구성 (TabBar + 라우팅 개편 + 프로그램명 변경)

> 작성일: 2026-06-11
> 담당: Sohee Kang
> 상태: 대기 (백엔드 의존 없음 — 독립 착수 가능)
> 이후: Iteration 09 (거래처 등록 관리 목록 페이지)
> 참조: `backend/docs/plan/member-management-ui-plan.md` 3·8-1·8-2

---

## 목표

단일 페이지(`/`) 구조를 탭 기반 멀티 페이지(`/goods`, `/members`)로 개편한다.
거래처 등록 관리 화면을 추가하기 위한 라우팅·레이아웃 기반을 마련하며, 기존 상품 관리 기능은 영향받지 않는다.

거래처 등록 관리가 추가되며 다음 두 가지를 함께 반영한다.

- **헤더 전역화**: 헤더는 전역 요소(로고 + 탭 + 아바타)만 유지한다. 상품 전용 컨트롤인 **상태 동기화 버튼·자동 실행 시간 설정**은 헤더에서 제거하고 상품 관리 탭 본문(`GoodsPage`) 상단으로 이동한다. (거래처 등록 관리와 무관하므로 탭 전환 시 노출되면 안 됨)
- **프로그램명 변경**: `에쓰푸드몰 판매 상태 자동화` → `에쓰푸드몰 운영 관리`. 상품·회원을 포괄하는 명칭으로 헤더 로고·로그인 화면·`index.html` title을 일괄 변경한다.

---

## 범위

### 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `src/App.tsx` | 라우트 재구성 + `AuthedLayout` 도입 |
| `src/pages/DashboardPage.tsx` → `src/pages/GoodsPage.tsx` | 파일명·컴포넌트명 변경 + 헤더에서 옮겨온 상태 동기화·자동 실행 컨트롤을 본문 상단에 수용 |
| `src/pages/DashboardPage.module.scss` → `src/pages/GoodsPage.module.scss` | 파일명 변경 + 본문 툴바 스타일 추가 |
| `src/components/layout/Header.tsx` | 상태 동기화·자동 실행 컨트롤 및 관련 props 제거(전역 요소만 유지), 로고 텍스트 변경 |
| `src/pages/LoginPage.tsx` | 로고/타이틀 텍스트 `에쓰푸드몰 운영 관리`로 변경 |
| `index.html` | `<title>` 텍스트 변경 |

### 신규 파일

| 파일 | 역할 |
|------|------|
| `src/components/layout/AuthedLayout.tsx` | Header(탭 포함) + `<Outlet />` 레이아웃 |
| `src/pages/MembersPage.tsx` | 거래처 등록 관리 플레이스홀더 (Iteration 09에서 구현) |

---

## 구현 명세

### App.tsx — 라우팅 재구성

```tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route element={<AuthedLayout />}>
    <Route path="/goods" element={<RequireAuth><GoodsPage /></RequireAuth>} />
    <Route path="/members" element={<RequireAuth><MembersPage /></RequireAuth>} />
  </Route>
  <Route path="/" element={<Navigate to="/goods" replace />} />
  <Route path="*" element={<Navigate to="/goods" replace />} />
</Routes>
```

기존 `RequireAuth` 컴포넌트는 변경 없이 재사용한다.

### AuthedLayout.tsx

탭은 `Header` 내부에 렌더링되므로 `AuthedLayout`은 `Header`와 `<Outlet />`만 렌더링한다.
`useLogout`, `useUserNotifications`, `getSession` 호출을 담당하며, 상태를 `Header` props로 전달한다.

```tsx
const AuthedLayout = () => {
  // useLogout, useUserNotifications, getSession 상태 관리
  return (
    <>
      <Header email={...} ... />
      <Outlet />
    </>
  );
};
```

기존 `GoodsPage` 안에 있던 `<Header />` 렌더링은 `AuthedLayout`으로 이동.

### 헤더 전역화 — 동기화 컨트롤 이동

`Header`는 `AuthedLayout`에서 전역으로 렌더링되므로, 상품 전용 컨트롤이 헤더에 남으면 거래처 등록 관리 탭에서도 노출된다. 따라서 다음을 옮긴다.

- `Header.tsx`에서 **상태 동기화 버튼**(`runBtn`)과 **자동 실행 시간 설정**(`autoRunWrap`/팝오버) 마크업·로직 제거
- `Header` Props에서 관련 항목 제거: `schedule`, `running`, `lastRefreshedAt`, `onRunNow`, `onApplyInterval` 등 동기화 전용 props
- 제거한 컨트롤을 `GoodsPage` 본문 **상단 툴바**로 이동(기존 `useBatch` 훅 연결은 GoodsPage에서 유지)
- 디자인은 `docs/design/design.md`의 주요 버튼·팝오버·입력 토큰 그대로 사용 (시각적 변경 없음, 위치만 이동)

헤더에 남는 전역 요소: 로고, TabBar(인접), 아바타 드롭다운(계정·알림·로그아웃).

### 프로그램명 변경

`에쓰푸드몰 판매 상태 자동화` → **`에쓰푸드몰 운영 관리`** 로 일괄 변경.

- `Header.tsx` 로고: `에쓰푸드몰 <em>운영 관리</em>` (강조부 `em`만 교체, 기존 구조 유지)
- `LoginPage.tsx` 헤더 로고/문구
- `index.html` `<title>`
- 텍스트 교체만 수행하며 색·레이아웃은 변경하지 않는다.

### TabBar.tsx

```tsx
const TABS = [
  { label: '상품 관리', path: '/goods' },
  { label: '거래처 등록 관리', path: '/members' },
] as const;

// <nav> + NavLink 사용 (role="tablist" 미사용 — 페이지 전환이므로 nav 패턴이 접근성상 올바름)
// 활성 탭: aria-current="page", --teal 하단 보더 2px, 텍스트 --text 강조
// /members/:memNo 진입 시에도 '거래처 등록 관리' 탭 활성 유지 (path prefix 매칭: end={false})
// 640px 이하: 탭이 전체 너비 균등 분할
```

`/members/:memNo` 진입 시에도 '거래처 등록 관리' 탭 활성 유지 (path prefix 매칭: `end={false}`)  
640px 이하: 탭이 전체 너비 균등 분할

### Header.module.scss — 탭 스타일

Header.module.scss에 탭 스타일을 추가한다. 별도 TabBar 컴포넌트 없음.

```scss
.hdrTabs {
  flex: 0 0 auto;
  margin-left: 16px;
  display: flex;
  gap: 2px;
  height: 100%;
  align-items: stretch;
}

.tabNav {
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-size: 14px;
  color: var(--text2);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  font-weight: 500;

  &:hover {
    color: var(--text);
  }
}

.active {
  color: var(--text);
  border-bottom-color: var(--teal);
  font-weight: 600;
}

@media (max-width: 640px) {
  .tabNav {
    flex: 1;
    text-align: center;
    padding: 12px 8px;
  }
}
```

### DashboardPage.tsx → GoodsPage.tsx

- 파일명: `DashboardPage.tsx` → `GoodsPage.tsx`
- 컴포넌트명: `DashboardPage` → `GoodsPage`
- `DashboardPage.module.scss` → `GoodsPage.module.scss`
- Header 렌더링 제거 (AuthedLayout으로 이동)
- 배치 제어 UI(상태 동기화 버튼, 자동 실행 팝오버)를 Header에서 GoodsPage 본문 상단 툴바(`.toolbar`)로 이동
- 배치 관련 로컬 상태(`intervalInput`, `showIntervalPopover`, `intervalWrapRef`) 및 SVG 아이콘(ClockIcon, PlayIcon, SpinnerIcon)도 GoodsPage로 이동

### MembersPage.tsx (플레이스홀더)

```tsx
// Iteration 09에서 전체 구현 — 지금은 빈 페이지
const MembersPage = () => (
  <div>거래처 등록 관리 (준비 중)</div>
);
export default MembersPage;
```

### Header.test.tsx

NavLink가 Header 내부에 포함되므로 Router context가 필요하다.  
모든 render 호출을 `<MemoryRouter>` 로 감싼다.

---

## 완료 기준

- `npm run build` — TypeScript 오류 없이 완료
- `npm run lint` — 오류 없이 통과
- 기존 Vitest 테스트 전체 통과 (`npm run test`)
- 수동 확인:
  - `/` 접근 → `/goods` 리다이렉트
  - `/goods` 진입 → TabBar 표시, '상품 관리' 탭 활성
  - TabBar '거래처 등록 관리' 클릭 → `/members` 이동, '거래처 등록 관리' 탭 활성
  - `/members/12345` 진입 → '거래처 등록 관리' 탭 활성 유지
  - 모바일(640px) → 탭 균등 분할
  - 기존 상품 관리 기능 (검색·필터·업로드·배치) 정상 동작
  - 상태 동기화·자동 실행 컨트롤이 **상품 관리 탭 본문**에만 표시되고 헤더·회원 탭에는 없음
  - 헤더 로고·로그인 화면·브라우저 탭 제목이 모두 `에쓰푸드몰 운영 관리`로 표시됨

---

## 주의 사항

- DashboardPage 이름 변경 시 import 참조 전체 업데이트 (`App.tsx` 등)
- 탭은 Header 내부에 있으므로 Header 테스트에 MemoryRouter 래퍼 필요
- Header에 NavLink 추가로 react-router-dom import 필요
- 기존 테스트 파일에 `DashboardPage` import가 있으면 `GoodsPage`로 변경
- MemberDetailPage는 Iteration 10에서 모달 방식으로 구현 — 이 Iteration에서 생성하지 않음
- 라우팅 외 기존 코드 변경 금지 (스코프 밖 리팩토링 금지)
