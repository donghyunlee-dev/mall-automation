# ATCS-225 Spec

## Feature Specification

- The product table sale status chip becomes clickable only for eligible goods.
- Eligible goods must be code-validated, transfer-allowed, and have a known sale status.
- Clicking an eligible sale status chip opens a force-sync confirmation modal.
- The modal explains the target state, which is always the opposite of the current local sale status.
- Confirming sends the force-sync API and reloads the goods list after the response.

## API Changes

- Use existing `POST /api/goods/godo-status/force-sync`.
- Request body:
  - `godoGoodsCodes: string[]`
  - `targetSoldOutYn: 'Y' | 'N'`
- Response body:
  - `items[]`
  - per item: `godoGoodsCode`, `result`, `message`, `soldOutYn`

## Database Changes

- None.

## Domain Model Changes

- None.
- Add frontend DTO types for force-sync request and response.

## Validation Rules

- Enable the action only when `status === 'NORMAL'`.
- Enable the action only when `erpSyncYn === 'Y'`.
- Enable the action only when `godoSyncYn === 'Y'`.
- Enable the action only when `godoSyncBlockedYn === 'N'`.
- Disable the action when sale status is unavailable.
- Send `targetSoldOutYn: 'Y'` when the current item is on sale.
- Send `targetSoldOutYn: 'N'` when the current item is sold out.

## Edge Cases

- If the backend returns `SKIPPED`, `FAILED`, or `NOT_FOUND`, show an error and do not treat the UI as updated.
- If the HTTP request fails, keep the modal open with an error message.
- If the current status is unavailable, the sale status chip must not open the modal.

## Exception Scenarios

- Godo API failure returns an item-level failure and should not be shown as success.
- Stale frontend state can cause a backend skip; the frontend must rely on response results, not the pre-click eligibility alone.

## Acceptance Criteria Mapping

- Happy path: eligible on-sale goods force-sync with `targetSoldOutYn: 'Y'`, then reload.
- Happy path: eligible sold-out goods force-sync with `targetSoldOutYn: 'N'`, then reload.
- Failure path: ineligible goods are not clickable.
- Failure path: non-success responses or HTTP errors show an error and do not optimistically update the table.
