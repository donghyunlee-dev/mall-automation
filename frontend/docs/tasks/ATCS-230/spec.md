---
task: ATCS-230
title: FE - 전체 상품 목록 고도몰 상품코드 기준으로 정렬
---

## Feature Specification

### 기본 정렬 변경

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| sortKey | `'updatedAt'` | `'godoGoodsCode'` |
| sortDir | `'desc'` | `'asc'` |

### 정렬·페이지 유지 동작 (기존 코드 확인)

`reload()` → `load()` → `setAllItems(res.items)` only
- `sortKey`, `sortDir`, `page` 상태 변경 없음 → 이미 유지됨
- 추가 코드 변경 불필요

## Acceptance Criteria Mapping

- [AC1] 기본 정렬이 고도몰 상품코드 오름차순 → `sortKey='godoGoodsCode'`, `sortDir='asc'`
- [AC2] 추가/수정 후 정렬 기준 유지 → `reload()`이 sort 상태 변경 안 함 (기존)
- [AC3] 추가/수정 후 페이지 유지 → `reload()`이 page 상태 변경 안 함 (기존)
