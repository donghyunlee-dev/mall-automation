# ATCS-224 Spec

## Feature Specification

- provide a manual API for force-sending sold-out status to Godo for selected goods
- accept a list of Godo goods codes and one target sold-out flag for the request
- process each selected goods item independently and return item-level results

## API Changes

- add `POST /api/goods/godo-status/force-sync`
- request body:
  - `godoGoodsCodes: string[]`
  - `targetSoldOutYn: Y|N`
- response body:
  - `items[]`
  - per item: `godoGoodsCode`, `result`, `message`, `soldOutYn`

## Database Changes

- none

## Domain Model Changes

- no schema change
- reuse `Goods` state fields and `GoodsStatusLog`

## Validation Rules

- request list must not be empty
- `targetSoldOutYn` must be `Y` or `N`
- eligible goods must satisfy:
  - `godoSyncYn == Y`
  - `godoGoodsCode` exists
  - `godoSyncBlockedYn == N`
  - `status` and `erpSyncYn` are not used as force-sync gates

## Edge Cases

- goods code not found returns an item-level `NOT_FOUND` style result
- ineligible goods return an item-level skipped result with message
- target state equal to current local state still force-sends to Godo and returns success when the Godo call succeeds

## Exception Scenarios

- Godo API failure must not update `soldOutYn` or status logs for the failed item
- one item failure must not abort the whole request

## Acceptance Criteria Mapping

- AC happy path: eligible goods force-sync to Godo without WMS policy checks and persist local updates
- AC failure path: blocked goods or non-`godoSyncYn` goods are excluded from transmission with explicit item result
- AC failure path: Godo API failure leaves local sold-out state and logs unchanged for that item
