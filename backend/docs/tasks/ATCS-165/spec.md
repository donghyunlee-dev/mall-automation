# ATCS-165 Spec

## Feature Specification

- `PUT /api/goods/{id}` updates goods name, ERP code, and scope as before
- if the requested ERP code is already assigned to another goods row, the API returns a conflict response instead of a generic internal error
- business-error responses return a summarized message that helps identify the failure cause

## API Changes

- success response remains `200 OK` with `GoodsResponse`
- duplicate update failure returns:
  - `409 Conflict`
  - shared `ErrorResponse`
  - message describing that the goods data is duplicated

## Database Changes

- no schema changes
- unique ERP code rule remains enforced at the application layer before persistence

## Domain Model Changes

- no domain entity change required
- update flow adds pre-save duplicate validation for ERP code collisions

## Validation Rules

- `goodsName`, `erpGoodsCode`, and `onlyMallYn` validation remain unchanged
- ERP code duplication is rejected when another goods owns the requested ERP code
- updating a goods with its existing ERP code remains allowed

## Edge Cases

- updating only `goodsName` with the same ERP code succeeds
- changing `onlyMallYn` still reloads the correct common status config
- updating a missing goods id still returns `404`

## Exception Scenarios

- invalid request body returns `400 Bad Request`
- missing or expired session returns `401 Unauthorized`
- duplicate ERP code update returns `409 Conflict` with a summarized message
- unexpected failures continue to return the shared internal server error response

## Acceptance Criteria Mapping

- `API 전송 시 품목 정보 업데이트 완료`:
  - verified by controller and use case tests for successful update flows
- `오류 발생 시 원인을 메시지에 요약해서 전달`:
  - verified by controller and handler tests for duplicate update failures
- `API 호출 시 200 응답`:
  - verified by the successful update controller test
- `API 호출 시 500 응답`:
  - replaced for the known duplicate update path by an explicit conflict response with a readable message
