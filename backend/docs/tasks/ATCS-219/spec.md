# ATCS-219 Spec

## Feature Specification

- when `CommonStatusConfig` is updated, all goods in the same logical scope must receive the new `outOfStockQty` and `safetyRate`
- `GENERAL` scope maps to goods with `onlyMallYn = 'N'`
- `EXCLUSIVE` scope maps to goods with `onlyMallYn = 'Y'`
- synchronization must happen in the same update flow as config persistence

## API Changes

- none

## Database Changes

- none

## Domain Model Changes

- none
- goods criteria continue to be stored on each goods row

## Validation Rules

- only goods whose scope matches the updated config are synchronized
- `onSaleQty` remains stored in `common_status_config` only and is not written to goods
- hidden goods are still synchronized because their stored criteria can later affect restored behavior

## Edge Cases

- no goods exist for the updated scope
- both scopes have existing goods and only one scope is updated
- hidden goods exist in the updated scope and must also receive new criteria

## Exception Scenarios

- missing config for the requested scope continues to raise the existing not-found behavior
- if goods persistence fails, the config update transaction must not partially succeed

## Acceptance Criteria Mapping

- updating `GENERAL` config synchronizes all `onlyMallYn='N'` goods and leaves `onlyMallYn='Y'` goods unchanged
- updating `EXCLUSIVE` config synchronizes all `onlyMallYn='Y'` goods and leaves `onlyMallYn='N'` goods unchanged
- subsequent batch evaluations use the synchronized `goods.availableQty` and `goods.safetyRate` values
