# ATCS-217 Spec

## Feature Specification

- Add a `상태 기준 설정` action to the dashboard product-list card header.
- Opening the action shows both `GENERAL` and `EXCLUSIVE` common status config values.
- The settings modal uses a compact table layout with `GENERAL` and `EXCLUSIVE` as rows and criteria as columns.
- Each row exposes:
  - `품절 기준 수량` as editable
  - `판매 기준 수량` as read-only, automatically computed as `품절 기준 수량 + 1`
  - `잔여율 기준` as editable
- Saving updates each scope through the existing backend API.
- Product add/edit modals display the current selected-scope config as reference text.

## API Changes

- No backend API changes.
- Frontend uses:
  - `GET /api/config/status`
  - `PUT /api/config/status/{scope}`

## Database Changes

- None.

## Domain Model Changes

- Add frontend domain types for:
  - `CommonStatusConfigScope`
  - `CommonStatusConfig`
  - `UpdateCommonStatusConfigBody`

## Validation Rules

- `outOfStockQty` and `safetyRate` are numeric and `0` or greater.
- `onSaleQty` is not user-editable and is always sent as `outOfStockQty + 1`.
- Save should not proceed while data is loading or a save is already in flight.

## Edge Cases

- Config query fails on dashboard entry.
- One or more expected scopes are missing from the API response.
- User opens product add/edit modal before config load completes.
- Save fails after optimistic local edits in the settings modal.

## Exception Scenarios

- On query failure, show an error state in the settings action area and modal.
- On save failure, keep the modal open and preserve user-entered values.
- If config data is not yet available, product add/edit reference text should not show misleading defaults.

## Acceptance Criteria Mapping

- AC1: Dashboard settings modal shows `GENERAL` and `EXCLUSIVE` values from API.
- AC2: Saving updates values and the refreshed dashboard/modal reference text reflects the latest config.
- AC3: Failures show a visible error and do not overwrite the existing in-memory config with invalid state.
