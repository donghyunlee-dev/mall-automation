# ATCS-167 Spec

## Feature Specification

- `PUT /api/config/status/{scope}` updates the common status config identified by the path variable
- the request body contains only editable fields: `outOfStockQty`, `onSaleQty`, and `safetyRate`
- the success response continues to return the updated config including its `scope`

## API Changes

- update endpoint path changes from `PUT /api/config/status` to `PUT /api/config/status/{scope}`
- `UpdateCommonStatusConfigRequest` no longer includes `scope`
- response contract remains `CommonStatusConfigResponse`

## Database Changes

- no schema changes
- existing `common_status_config.scope` remains the lookup key used by the update flow

## Domain Model Changes

- no domain model changes
- the selected `GoodsScope` is provided by the controller path instead of the request body

## Validation Rules

- `scope` must be supplied through the URL path
- `outOfStockQty`, `onSaleQty`, and `safetyRate` must remain zero or greater
- the response must reflect the same scope that was requested in the URL

## Edge Cases

- requests with missing path scope do not match the update endpoint
- requests that omit `scope` from the body remain valid because the field is no longer part of the contract
- unknown scope values continue to fail through the existing exception path

## Exception Scenarios

- missing or expired session still returns `401 Unauthorized`
- invalid numeric values in the body still return `400 Bad Request`
- unknown scope or missing persisted config continues to surface through the existing service error behavior

## Acceptance Criteria Mapping

- `식별자를 REST API 형식으로 URL에 포함`:
  - verified by controller test coverage for `PUT /api/config/status/{scope}`
- `Body에 식별자 제거`:
  - verified by request DTO and controller tests that send only editable fields
- `식별자(scope)를 제외하고 수정 시 저장`:
  - verified by use case and controller tests that update `GENERAL` through the path variable
- `URL, Body 변경 확인`:
  - reflected in the API spec and task result notes
