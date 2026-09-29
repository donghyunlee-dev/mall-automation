---
task: ATCS-231
title: FE - 뱃지 디자인 변경
---

## Feature Specification

### ProductTable 뱃지

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| `.badgeType` | `width: 46px` | 제거 (auto) |
| `.badgeStatus` | `width: 58px` | 제거 (auto) |
| `.badgeTransfer` | `width: 54px` | 제거 (auto) |
| `.badgeCode` | `width: 76px` | 제거 (auto) |
| `.badgeStatus` font-weight | (`.badge`의 600 상속) | `font-weight: 500` |

### HistoryTable 뱃지

모든 뱃지(`.bG`, `.bE`, `.bOn`, `.bOut`)를 ProductTable `.badge` 기준으로 통일:

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| display | `inline-block` | `inline-flex` + `align-items/justify-content: center` |
| height | (미지정) | `24px` |
| padding | `2px 6-7px` | `0 8px` |
| border-radius | `var(--r-sm)` | `999px` |
| font-size `.bOn/.bOut` | `11px` | `10px` |

### HistoryTable 행 높이

td 패딩은 이미 ProductTable과 동일(`7px 11px`). 뱃지 높이를 24px로 통일하면 행 높이가 자연스럽게 맞춰짐.

## Acceptance Criteria Mapping

- [AC1] 상품 목록 뱃지들의 너비가 글자수에 맞게 축소됨 → 고정 width 제거
- [AC2] 판매상태 뱃지 font-weight 경감으로 가독성 향상 → `font-weight: 500`
- [AC3] 변경이력 뱃지 높이·폰트스타일이 상품목록 뱃지와 동일 → height 24px, pill shape
- [AC4] 변경이력 행 높이가 상품목록 행 높이와 동일 → 뱃지 높이 통일로 해결
