# ATCS-211 Spec

## Feature Specification

- a Godo goods search response should only mark a goods row as Godo-synced when the response contains a `goodsNo` equal to the searched goods code
- when `code=000` is returned but the response `return` is empty, the goods row must keep or reset `godo_sync_yn` to `N`
- when no matching goods is returned, the stored sold-out flag should remain normalized to `N`

## API Changes

- none

## Database Changes

- none

## Domain Model Changes

- goods registration must distinguish between transport-level Godo success and actual goods match success
- the goods model may need a minimal method to apply either a matched or unmatched Godo search result

## Validation Rules

- matching requires the returned Godo `goodsNo` to equal the requested goods code
- blank or different `goodsNo` values must not set `godo_sync_yn` to `Y`
- transport failures still follow the current `GodoApiException` path

## Edge Cases

- `code=000` with `<return/>` and no `goods_data`
- `code=000` with a `goodsNo` different from the searched goods code
- hidden goods restore where the previous row had already been Godo-synced

## Exception Scenarios

- non-`000` Godo response codes still throw `GodoApiException`
- duplicate visible goods and duplicate ERP code behavior remain unchanged

## Acceptance Criteria Mapping

- AC: update `godo_sync_yn` to `Y` only when the same Godo goods ID is present in the response
  - verify matching `goodsNo` before applying Godo sync to the goods row
- AC: handle `code=000` with no item data as unsynced
  - keep or reset `godo_sync_yn` to `N` when the response has no matching goods payload
- AC: empty `return` with `godo_sync_yn='Y'` is a failure
  - add focused tests for empty and mismatched responses and keep them passing
