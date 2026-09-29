# ATCS-222 Spec

## Feature Specification

- goods can store whether automatic Godo sold-out transmission is blocked
- the frontend can toggle the setting through a dedicated authenticated API
- the backend rejects enabling the block when the goods status is not `NORMAL`
- the product sync batch skips blocked goods before calling the Godo sold-out status API

## API Changes

- add `PATCH /api/goods/{id}/godo-sync-block`
- request body:
  - `blockedYn`: `Y` or `N`
- response body:
  - updated `GoodsResponse`
- include `godoSyncBlockedYn` in `GoodsResponse`

## Database Changes

- add `godo_sync_blocked_yn TEXT NOT NULL DEFAULT 'N'` to `goods`

## Domain Model Changes

- store the automatic transmission block flag on `Goods`
- add domain/application behavior to change the flag with validation

## Validation Rules

- `blockedYn` must be `Y` or `N`
- enabling the block (`Y`) is allowed only when `goods.status == NORMAL`
- disabling the block (`N`) is always allowed for an existing goods item

## Edge Cases

- a blocked goods item remains visible and keeps its existing business status
- a goods item with matching WMS stock is still skipped when the block is enabled
- a non-`NORMAL` goods item can still be returned by the API with `godoSyncBlockedYn = N`

## Exception Scenarios

- missing goods id returns `GOODS_NOT_FOUND`
- invalid request payload returns `400`
- enabling the block for a non-`NORMAL` goods item returns a business validation error and does not update the goods

## Acceptance Criteria Mapping

- requirement 1: persisted flag and update API
- requirement 2: validation for `NORMAL` status only
- requirement 3: batch skip before Godo API call
