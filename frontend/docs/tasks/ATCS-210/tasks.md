# ATCS-210 Tasks

## Development Tasks

- [x] 판매 상태 기준을 고도몰 연동 기준으로 공용화
- [x] 상품 목록 판매 상태 뱃지 조건 수정
- [x] 상품 목록 상태 필터 조건 수정
- [x] 대시보드 품절 집계 조건 수정
- [x] 회귀 테스트 추가

## Test Cases

- [x] `godoSyncYn = N` 이면 `확인불가`로 판별된다
- [x] `godoSyncYn = Y`, `soldOutYn = Y` 이면 `품절`로 판별된다
- [x] `godoSyncYn = Y`, `soldOutYn = N` 이면 `판매중`으로 판별된다
- [x] `erpSyncYn = N` 이어도 `godoSyncYn = Y`이면 `확인불가`로 판별되지 않는다

## Modules To Modify

- `src/components/dashboard/ProductTable.tsx`
- `src/hooks/useGoods.ts`
- `src/pages/DashboardPage.tsx`
- `src/utils/goodsSaleStatus.ts`
- `src/utils/goodsSaleStatus.test.ts`

## Integration Points

- Dashboard 전체 상품 목록
- Dashboard 요약 카드 품절 집계

## Result

### Changed Files

- `docs/tasks/ATCS-210/plan.md`
- `docs/tasks/ATCS-210/spec.md`
- `docs/tasks/ATCS-210/tasks.md`
- `src/utils/goodsSaleStatus.ts`
- `src/utils/goodsSaleStatus.test.ts`
- `src/hooks/useGoods.ts`
- `src/hooks/useGoods.test.tsx`
- `src/components/dashboard/ProductTable.tsx`
- `src/pages/DashboardPage.tsx`
- `src/App.tsx`
- `src/components/dashboard/ProductSearchBar.tsx`

### Summary

- 판매 상태 계산 기준을 `godoSyncYn` 중심으로 공용 유틸에 모았다.
- 상품 목록의 판매 상태 뱃지, 상태 필터, 대시보드 품절 집계가 동일 규칙을 사용하도록 수정했다.
- `erpSyncYn = N` 이더라도 고도몰 연동 상품은 `품절` 또는 `판매중`으로 보이도록 회귀 테스트를 추가했다.

### Test Results

- `npm test -- --run` ✅ 통과
- `npm run build` ✅ 통과
- `npm run lint` ✅ 통과

### Review Notes

- 기존 브랜치에 있던 `JSX.Element` 타입 오류와 사용되지 않는 props 정리는 검증 통과를 위한 최소 수정으로 함께 반영했다.
- `src/components/modals/ProductEditModal.tsx`의 사용자 수정 내용은 그대로 유지했다.
