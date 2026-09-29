# ATCS-164 Spec

## Feature Specification

- `POST /api/goods` accepts a JSON array of `RegisterGoodsRequest`
- each request item contains:
  - `godoGoodsCode`
  - `erpGoodsCode`
  - `goodsName`
  - `onlyMallYn`
- the API returns a list response containing one registration result per saved item

## API Changes

- request body changes from:
  - single object
- request body changes to:
  - array of objects
- success response changes from:
  - single `RegisterGoodsResponse`
- success response changes to:
  - `RegisterGoodsListResponse { items: [...] }`

## Database Changes

- no schema changes
- existing `goods` persistence flow is reused for each item in the list

## Domain Model Changes

- no domain entity change required
- application use case is extended to iterate over request items and collect per-item results

## Validation Rules

- bean validation must still run for each request item
- existing duplicate checks by Godo code and ERP code remain in effect
- existing hidden-goods restore behavior remains in effect

## Edge Cases

- a one-item array must still register correctly
- a multi-item array must save all entries when all are valid
- duplicate persisted goods must still fail with the existing duplicate error

## Exception Scenarios

- invalid request item fields return `400 Bad Request`
- missing or expired session returns `401 Unauthorized`
- duplicate goods data returns `409 Conflict`
- unexpected failures continue to return the shared internal server error response

## Acceptance Criteria Mapping

- `1개 품목 정보 저장 시 정상적으로 저장된다.`:
  - verified by controller and use case tests using a one-item array
- `1개 이상 품목 정보 저장 시 정상적으로 저장된다.`:
  - verified by use case and integration coverage using a multi-item array
- `품목 저장 시 오류가 발생한다.`:
  - existing exception handling path remains unchanged
- `품목 저장 시 1개만 저장된다.`:
  - use case returns one result per request item and saves each item
- `전체 품목 저장이 되지 않는다.`:
  - list processing runs inside one transactional use case flow
