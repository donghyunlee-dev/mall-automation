# Iteration 04 Plan — 상품 목록 테이블

> 작성일: 2026-04-01  
> 담당: Sohee Kang  
> 상태: In Progress

---

## 목표

전체 상품 목록 테이블을 구현한다. 검색, 필터(품절/판매중/범용/전용), 정렬, 페이지네이션(10개씩)을 포함한다.

---

## 범위

| # | 항목 | 파일 |
|---|---|---|
| 1 | 상품 목록 훅 | `src/hooks/useGoods.ts` |
| 2 | 검색바 컴포넌트 | `src/components/dashboard/ProductSearchBar.tsx` |
| 3 | 검색바 스타일 | `src/components/dashboard/ProductSearchBar.module.scss` |
| 4 | 상품 테이블 컴포넌트 | `src/components/dashboard/ProductTable.tsx` |
| 5 | 상품 테이블 스타일 | `src/components/dashboard/ProductTable.module.scss` |
| 6 | 페이지네이션 컴포넌트 | `src/components/dashboard/Pagination.tsx` |
| 7 | 페이지네이션 스타일 | `src/components/dashboard/Pagination.module.scss` |
| 8 | DashboardPage 업데이트 | 상품 목록 섹션에 ProductTable 통합 |

---

## 구현 계획

1. `src/hooks/useGoods.ts` — 목록 조회, 프론트 필터/정렬/페이지 관리 — Done
2. `ProductSearchBar.tsx` + 스타일 — Done
3. `ProductTable.tsx` + 스타일 — Done
4. `Pagination.tsx` + 스타일 — Done
5. DashboardPage에서 ProductTable 통합 — Done
6. `npm run build` 검증 — Done
7. `npm run lint` 검증 — Done

---

## 완료 기준

- UI가 `docs/design/mockup.html`의 상품 목록 테이블과 일치
- 검색/필터/정렬/페이지네이션 정상 동작
- API 오류 상태 처리
- `npm run build` + `npm run lint` 오류 없음
