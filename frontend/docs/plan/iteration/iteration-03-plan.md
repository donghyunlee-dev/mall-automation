# Iteration 03 Plan — 대시보드 레이아웃

> 작성일: 2026-04-01  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

Header, 현황 요약 카드 3개, 기본 레이아웃을 구성한다. 세션 없이 접근 시 `/login` 리다이렉트 처리 포함.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | 배치 타입 | `src/types/batch.ts` |
| 2 | 상품 타입 (요약용) | `src/types/goods.ts` |
| 3 | 배치 API 함수 | `src/api/batch.ts` |
| 4 | 상품 API 함수 | `src/api/goods.ts` |
| 5 | 배치 상태 훅 | `src/hooks/useBatch.ts` |
| 6 | 헤더 컴포넌트 | `src/components/layout/Header.tsx` |
| 7 | 헤더 스타일 | `src/components/layout/Header.module.scss` |
| 8 | 아바타 드롭다운 | `src/components/layout/AvatarDropdown.tsx` |
| 9 | 아바타 드롭다운 스타일 | `src/components/layout/AvatarDropdown.module.scss` |
| 10 | 현황 요약 카드 | `src/components/dashboard/SummaryCards.tsx` |
| 11 | 현황 요약 카드 스타일 | `src/components/dashboard/SummaryCards.module.scss` |
| 12 | 대시보드 페이지 | `src/pages/DashboardPage.tsx` |
| 13 | 대시보드 스타일 | `src/pages/DashboardPage.module.scss` |

---

## 범위 외

- 상품 목록 테이블 (Iteration 04)
- 상품 관리 모달 (Iteration 05)
- 배치 관리 모달 / 변경 이력 테이블 (Iteration 06)
- 로그아웃 API 연동 (백엔드 미구현 — UI만 제공)

---

## 구현 계획

1. 타입 정의 (`batch.ts`, `goods.ts`) — Done
2. API 함수 (`batch.ts`, `goods.ts`) — Done
3. `useBatch.ts` 훅 — Done
4. `Header.tsx` + 스타일 — Done
5. `AvatarDropdown.tsx` + 스타일 — Done
6. `SummaryCards.tsx` + 스타일 — Done
7. `DashboardPage.tsx` 레이아웃 + 세션 확인 — Done
8. `npm run build` 검증 — Done
9. `npm run lint` 검증 — Done

---

## 완료 기준

- UI가 `docs/design/mockup.html`의 Header 및 요약 카드와 일치
- 세션 없이 접근 시 `/login` 리다이렉트
- `npm run build` + `npm run lint` 오류 없음
