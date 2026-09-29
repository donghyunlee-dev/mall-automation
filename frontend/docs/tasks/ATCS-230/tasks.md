---
task: ATCS-230
title: FE - 전체 상품 목록 고도몰 상품코드 기준으로 정렬
---

## Development Tasks

- [x] `useGoods.ts` sortKey 기본값 `'updatedAt'` → `'godoGoodsCode'`
- [x] `useGoods.ts` sortDir 기본값 `'desc'` → `'asc'`

## Result

### Changed Files

- `frontend/src/hooks/useGoods.ts`
- `frontend/docs/tasks/ATCS-230/plan.md`
- `frontend/docs/tasks/ATCS-230/spec.md`
- `frontend/docs/tasks/ATCS-230/tasks.md`

### Test Results

- `npm run build` — 성공

### Review Notes

- 2줄 변경으로 요구사항 전체 충족
- reload() 후 sort/page 유지는 기존 코드에서 이미 동작함
