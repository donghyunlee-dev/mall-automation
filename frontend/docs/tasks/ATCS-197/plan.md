# ATCS-197 Plan

## Requirement Summary
전체 상품 목록 필터에 '판매중' 버튼 오른쪽에 '확인불가' 필터 버튼 추가.
비활성 시 기존 버튼과 동일한 스타일, 활성 시 엠버색(#d97706)으로 변하며 판매상태 UNVERIFIED 품목만 필터.

## Business Context
ATCS-195에서 판매 상태 뱃지에 확인불가(UNVERIFIED) 상태가 추가됨.
해당 상태 품목을 필터로 조회할 수 없으므로 필터 버튼 추가 필요.

## Scope
- `FilterKey` 타입에 `'unavailable'` 추가
- `useGoods` hook의 `applyFilter`에 unavailable 케이스 추가 (status === 'UNVERIFIED')
- `ProductSearchBar` FILTER_BUTTONS 배열에 확인불가 버튼 추가 (판매중 오른쪽)
- `ProductSearchBar.module.scss`에 활성 스타일 `.fu` 추가 (amber #d97706)

## Out of Scope
- 백엔드 변경 없음
- 뱃지 스타일 변경 없음

## Impacted Modules
- `src/hooks/useGoods.ts`
- `src/components/dashboard/ProductSearchBar.tsx`
- `src/components/dashboard/ProductSearchBar.module.scss`

## Risk Analysis
낮음. 기존 필터 패턴의 단순 확장.
