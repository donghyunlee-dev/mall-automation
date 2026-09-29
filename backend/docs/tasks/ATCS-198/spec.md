# ATCS-198 Spec

## Feature Specification

- `PUT /api/goods/{id}` must require `goodsName`, `erpGoodsCode`, `onlyMallYn`, `availableQty`, and `safetyRate`.
- When validation passes, the API updates the target goods record with the supplied criteria values.
- When either newly required numeric field is missing, null, or negative, the API returns `400 Bad Request`.

## API Changes

- Request contract for `PUT /api/goods/{id}` treats `availableQty` and `safetyRate` as required numeric fields.
- Success examples must reflect the submitted criteria values in the response body.

## Database Changes

- No schema change.
- Existing `goods.available_qty` and `goods.safety_rate` columns continue to store the update values.

## Domain Model Changes

- No domain model shape change.
- Existing `Goods.update(...)` behavior remains the persistence path for criteria updates.

## Validation Rules

- `goodsName`: required, non-blank.
- `erpGoodsCode`: required, non-blank.
- `onlyMallYn`: required, `Y` or `N`.
- `availableQty`: required, integer, `>= 0`.
- `safetyRate`: required, integer, `>= 0`.

## Edge Cases

- Missing `availableQty` or `safetyRate` in JSON must fail instead of defaulting to `0`.
- Explicit `null` for either field must fail.
- `0` remains valid for both fields.

## Exception Scenarios

- Duplicate ERP goods code returns `409 Conflict`.
- Missing, null, or negative criteria fields return `400 Bad Request`.
- Unknown goods id returns `404 Not Found`.

## Acceptance Criteria Mapping

- AC1: Additional fields are accepted by the update API.
- AC2: Submitted `availableQty` and `safetyRate` values are persisted through the existing update flow.
- AC3: API documentation reflects the required fields and response examples.
- AC4: Requests with missing or null criteria fields fail with `400`.
