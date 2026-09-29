---
task: ATCS-231
title: FE - 뱃지 디자인 변경
status: in-progress
---

## Requirement Summary

전체 상품 목록과 일자별 변경 이력 테이블의 뱃지 디자인을 개선한다.

## Business Context

고정 너비 뱃지가 글자수에 비해 너무 넓어 시각적으로 어색하고, 판매상태 뱃지의 폰트 굵기가 과도하여 가독성이 떨어진다. 변경 이력 뱃지도 상품 목록 뱃지와 높이·스타일이 달라 일관성이 없다.

## Scope

- ProductTable 뱃지 너비 고정값 제거 (글자수 기반 auto-sizing)
- ProductTable 판매상태 뱃지 font-weight 600 → 500
- HistoryTable 뱃지 높이·폰트스타일을 ProductTable 기준에 맞춤
- HistoryTable 행 높이를 ProductTable 행 높이와 동일하게 맞춤

## Out of Scope

- 뱃지 색상 변경
- 뱃지 텍스트 변경
- 새로운 뱃지 추가

## Impacted Modules

- `src/components/dashboard/ProductTable.module.scss`
- `src/components/dashboard/HistoryTable.module.scss`

## Risk Analysis

CSS-only 변경. 레이아웃 깨짐 없는지 빌드 후 확인 필요.
