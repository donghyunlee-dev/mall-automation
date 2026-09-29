# ATCS-220 Tasks

## Development Tasks

- [x] 수정 모달에서 구분 변경 시 기준 값 자동 반영 처리
- [x] 상품 목록 reload 흐름을 await 가능하게 정리
- [x] 상태 기준 저장 후 상품 목록 재조회 연결

## Test Cases

- [x] 구분 변경 시 `availableQty`와 `safetyRate`가 선택 scope 기준으로 바뀌는지 검증
- [x] 구분 변경 후 저장 payload에 자동 반영된 값이 포함되는지 검증
- [ ] 상태 기준 저장 후 수정 팝업 재오픈 시 최신 데이터 표시 통합 검증

## Modules To Modify

- `src/components/modals/ProductEditModal.tsx`
- `src/pages/DashboardPage.tsx`
- `src/hooks/useGoods.ts`
- `src/components/modals/ProductEditModal.test.tsx`

## New Classes Or Components

- 없음

## Integration Points

- `useCommonStatusConfig` 저장 완료 후 `useGoods.reload`
- `ProductEditModal` 구분 선택 UI와 상품 수정 API payload

## Result

### Changed Files

- `docs/tasks/ATCS-220/plan.md`
- `docs/tasks/ATCS-220/spec.md`
- `docs/tasks/ATCS-220/tasks.md`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/ProductEditModal.test.tsx`
- `src/hooks/useGoods.ts`
- `src/pages/DashboardPage.tsx`

### Summary

- 구분 변경 시 상태 기준 값을 즉시 반영하고, 상태 기준 저장 뒤 상품 목록 재조회를 기다리도록 수정했다.

### Test Results

- `npm test -- --run src/components/modals/ProductEditModal.test.tsx` 통과
- `npm run build` 통과
- `npm run lint` 통과

### Review Notes

- 상태 기준 저장 후 재조회 완료 전 다른 작업이 시작되지 않도록 `reload` 호출을 비동기화했다.
- Vite build에서 기존 번들 크기 경고(`index-DzEDhFBM.js` 500kB 초과)는 유지되며 이번 이슈 범위 밖이다.
