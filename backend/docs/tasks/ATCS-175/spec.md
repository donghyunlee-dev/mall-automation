# ATCS-175 Spec

## Feature Specification

- `PUT /api/goods/{id}` accepts `availableQty` and `safetyRate` in the request body
- goods updates persist the provided criteria values to the `goods` table together with the other editable fields
- the update response continues to return the full goods payload including the updated criteria values

## API Changes

- request body for `PUT /api/goods/{id}` adds:
  - `availableQty`
  - `safetyRate`
- response schema is unchanged

## Database Changes

- no schema changes
- existing `goods.available_qty` and `goods.safety_rate` columns are updated during goods edits

## Domain Model Changes

- no new domain types
- goods update behavior changes so explicit request criteria values are stored instead of being recalculated from common scope config

## Validation Rules

- `goodsName` remains required
- `erpGoodsCode` remains required
- `onlyMallYn` remains `Y` or `N`
- `availableQty` must be zero or greater
- `safetyRate` must be zero or greater

## Edge Cases

- updating only criteria values while keeping the same ERP code
- updating criteria values together with `onlyMallYn`
- duplicate ERP code checks must continue to reject edits that conflict with another goods item

## Exception Scenarios

- duplicate ERP code still returns a conflict error
- invalid numeric criteria values still return a bad request before the use case runs
- missing target goods still returns not found

## Acceptance Criteria Mapping

- `API 2개 필드 추가`:
  - satisfied by adding `availableQty` and `safetyRate` to `UpdateGoodsRequest`
- `DB - goods 테이블에 데이터 수정`:
  - satisfied by persisting the request criteria values in the update use case
- `수정 후 조회 시 반영되었으면 통과`:
  - covered by update tests asserting the response contains the edited criteria values
- `API 문서 업데이트 필수`:
  - satisfied by updating the product API documentation for `PUT /api/goods/{id}`
