# ATCS-199 Tasks

## Development Tasks

- [ ] 수정 모달에 `availableQty`, `safetyRate` 입력 추가
- [ ] 저장 요청 body 확장
- [ ] 초기값/0값 처리 추가
- [ ] 테스트 추가

## Test Cases

- [ ] 초기 렌더에서 두 기준값이 표시된다
- [ ] 저장 시 두 기준값이 API body에 포함된다
- [ ] `0` 값도 정상 저장된다

## Modules To Modify

- `src/components/modals/ProductEditModal.tsx`
- `src/types/goods.ts`
- `src/components/modals/ProductEditModal.test.tsx`

## Integration Points

- `PUT /api/goods/{id}`
- Dashboard 상품 수정 모달 열기

## Result

### Changed Files

- `docs/tasks/ATCS-199/plan.md`
- `docs/tasks/ATCS-199/spec.md`
- `docs/tasks/ATCS-199/tasks.md`
- `src/types/goods.ts`
- `src/components/modals/ProductEditModal.tsx`
- `src/components/modals/ProductEditModal.test.tsx`

### Summary

- 상품 수정 모달에 `가용재고 기준`, `잔여율 기준` 입력을 추가했다.
- 기존 조회 데이터 값을 초기값으로 표시하고 저장 요청 body에 함께 포함했다.
- `0` 값도 정상 저장되도록 숫자 입력을 정규화했다.

### Test Results

- `npm test -- --run` ✅ 통과
- `npm run build` ✅ 통과
- `npm run lint` ✅ 통과

### Review Notes

- 두 신규 입력은 현재 `GoodsItem` props 값으로 초기화한다.
- 저장 요청은 기존 `updateGoods` API 함수를 유지하고 body만 확장했다.
