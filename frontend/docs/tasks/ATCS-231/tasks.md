---
task: ATCS-231
title: FE - 뱃지 디자인 변경
---

## Development Tasks

- [x] ProductTable.module.scss — badgeType/badgeStatus/badgeTransfer/badgeCode 고정 width 제거
- [x] ProductTable.module.scss — badgeStatus font-weight: 500 추가
- [x] HistoryTable.module.scss — bG/bE/bOn/bOut height: 24px, inline-flex, padding 0 8px, border-radius 999px, font-size 10px 통일

## Modules to Modify

- `src/components/dashboard/ProductTable.module.scss`
- `src/components/dashboard/HistoryTable.module.scss`

## Result

### Changed Files

- `frontend/src/components/dashboard/ProductTable.module.scss`
- `frontend/src/components/dashboard/HistoryTable.module.scss`
- `frontend/docs/tasks/ATCS-231/plan.md`
- `frontend/docs/tasks/ATCS-231/spec.md`
- `frontend/docs/tasks/ATCS-231/tasks.md`

### Test Results

- `npm run build` — 성공

### Review Notes

- CSS-only 변경
- TSX 파일 변경 없음
