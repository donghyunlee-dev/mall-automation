# ATCS-197 Tasks

## Development Tasks
- [x] `FilterKey` 타입에 `'unavailable'` 추가 (`useGoods.ts`)
- [x] `applyFilter`에 `'unavailable'` 케이스 추가
- [x] `FILTER_BUTTONS`에 확인불가 항목 추가 (`ProductSearchBar.tsx`)
- [x] `.fu` 활성 스타일 추가 (`ProductSearchBar.module.scss`)

## Modules Modified
- `src/hooks/useGoods.ts`
- `src/components/dashboard/ProductSearchBar.tsx`
- `src/components/dashboard/ProductSearchBar.module.scss`

## Result

### Changed Files
- `frontend/src/hooks/useGoods.ts`
- `frontend/src/components/dashboard/ProductSearchBar.tsx`
- `frontend/src/components/dashboard/ProductSearchBar.module.scss`
- `frontend/docs/tasks/ATCS-197/plan.md`
- `frontend/docs/tasks/ATCS-197/spec.md`
- `frontend/docs/tasks/ATCS-197/tasks.md`

### Summary
FilterKey에 'unavailable' 추가, applyFilter에 UNVERIFIED 필터 로직 추가,
ProductSearchBar에 확인불가 버튼 추가, 활성 시 엠버 스타일 적용.

### Test Results
- 타입 검사: 통과
- 빌드: 통과
