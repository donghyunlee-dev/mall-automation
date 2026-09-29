# ATCS-180 Spec

## Feature Specification

- when a goods registration request restores an existing hidden goods record, the use case must refresh the Godo sold-out status before saving
- the refreshed status must keep `godoSyncYn` aligned with the Godo lookup and persist the returned sold-out value
- the Godo goods search parser must support the live XML payload shape where `goodsNo`, `goodsNm`, and `soldOutFl` are nested under `data -> return -> goods_data`

## API Changes

- none

## Database Changes

- none

## Domain Model Changes

- none expected beyond using the existing `applyGodoSyncResult` behavior during the restore path
- none for the parser follow-up; the fix stays in the external adapter layer

## Validation Rules

- duplicate visible goods must still fail with `DuplicateGoodsException`
- hidden goods re-registration must still restore the goods to `NORMAL`

## Edge Cases

- a restored hidden goods record should persist `soldOutYn = Y` when the Godo API reports sold out
- a restored hidden goods record should persist `soldOutYn = N` when the Godo API reports on sale
- a live Godo XML payload using `<msg>` instead of `<message>` should still preserve a successful result message for logging

## Exception Scenarios

- Godo API lookup failures should continue following the existing registration error behavior
- missing session still remains outside this task and keeps the current controller handling

## Acceptance Criteria Mapping

- AC: registration reflects the Godo goods sold-out state
  - apply the Godo sold-out mapping in the restore branch before saving
- AC: registration is considered failed when Godo status is not reflected
  - cover the restore path with a focused test that proves the persisted `soldOutYn` changes
- AC: live Godo search responses must drive the stored sold-out state
  - parse the production XML structure and cover it with a focused adapter test
