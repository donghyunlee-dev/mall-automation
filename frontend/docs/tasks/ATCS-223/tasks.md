# ATCS-223 Tasks

## Development Tasks

- [x] `GoodsItem` 타입과 goods API 응답에 `godoSyncBlockedYn` 반영
- [x] 전송 차단 PATCH API 함수 추가
- [x] 상품 목록 필터에 전송 차단 토글 추가
- [x] 상품 목록 테이블에 전송 차단 컬럼 및 상태 배지 추가
- [x] 행 단위 차단/해제 확인 모달 추가
- [x] 대시보드에서 차단/해제 저장 및 토스트 흐름 연결
- [x] 관련 테스트 추가 및 갱신

## Test Cases

- [x] 차단 상태 필터가 차단 품목만 반환한다
- [x] 테이블이 허용/차단 상태와 액션을 올바르게 표시한다
- [x] 차단 불가 조건에서 버튼이 비활성화된다

## Modules To Modify

- `src/types/goods.ts`
- `src/api/goods.ts`
- `src/hooks/useGoods.ts`
- `src/components/dashboard/ProductSearchBar.tsx`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`
- `src/components/modals/Modal.module.scss`
- `src/pages/DashboardPage.tsx`

## New Components

- `src/components/modals/GodoSyncBlockConfirmModal.tsx`

## Integration Points

- Goods list query response
- Goods patch API for sync block
- Dashboard toast and reload flow

## Result

### Changed Files

- `docs/tasks/ATCS-223/plan.md`
- `docs/tasks/ATCS-223/spec.md`
- `docs/tasks/ATCS-223/tasks.md`
- `src/types/goods.ts`
- `src/api/goods.ts`
- `src/hooks/useGoods.ts`
- `src/hooks/useGoods.test.tsx`
- `src/components/dashboard/ProductSearchBar.tsx`
- `src/components/dashboard/ProductSearchBar.module.scss`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`
- `src/components/dashboard/ProductTable.test.tsx`
- `src/components/modals/GodoSyncBlockConfirmModal.tsx`
- `src/components/modals/Modal.module.scss`
- `src/pages/DashboardPage.tsx`
- `src/components/modals/ProductEditModal.test.tsx`
- `src/utils/exportGoodsExcel.test.ts`
- `src/utils/goodsSaleStatus.test.ts`

### Summary

- 상품 목록 데이터 모델에 `godoSyncBlockedYn`과 전송 차단 PATCH API를 추가했다.
- 상품 목록 검색 바에 전송 차단 필터를 추가하고, 테이블에 전송 차단 컬럼과 행 단위 차단/해제 액션을 구현했다.
- 확인 모달과 대시보드 저장 흐름을 연결해 성공 시 재조회, 실패 시 상태 유지가 되도록 맞췄다.
- 닫힌 PR 피드백을 반영해 미검증 품목은 `불가`로 표시하고, 검증 완료 품목만 `허용/차단` 판정에 포함되도록 UI를 조정했다.
- 추가 피드백을 반영해 `전송 여부` 컬럼에서 `허용/차단` 상태 자체를 클릭해 변경하도록 단순화하고, 별도 `변경` 및 `미검증` 텍스트를 제거했다.

### Test Results

- `npm test -- --run src/hooks/useGoods.test.tsx src/components/dashboard/ProductTable.test.tsx src/components/modals/ProductEditModal.test.tsx src/utils/exportGoodsExcel.test.ts src/utils/goodsSaleStatus.test.ts`
- `npm run build`
- `npm run lint`

### Review Notes

- 빌드는 통과했고, 번들 크기 경고는 기존 Vite chunk warning 수준으로 유지된다.
- 차단 활성화 가능 조건은 백엔드 명세에 맞춰 `status === 'NORMAL'`일 때만 UI에서 허용한다.
- 닫힌 PR #78 피드백 기준으로 전송 차단 컬럼은 `허용/차단/불가` 단일 판독값과 `변경` 액션으로 정리했다.
- 후속 코멘트 기준으로 `전송 차단` 헤더를 `전송 여부`로 변경했고, 컬럼 폭도 재조정했다.
