# ATCS-176 Spec

## Feature Specification

- sold-out checks aggregate all registered ERP-code WMS rows and compare the summed qty to `goods.availableQty`
- on-sale recovery checks aggregate only the WMS rows whose `VALID_PERCENT` is greater than or equal to the matched goods item's `safetyRate`
- transition decisions use per-goods criteria values instead of fixed values or common-config values

## API Changes

- no public API changes

## Database Changes

- no schema changes
- no data migration changes
- existing `goods.available_qty` and `goods.safety_rate` values become the active batch judgment criteria

## Domain Model Changes

- no new domain entities
- `GoodsSoldOutPolicy` judgment rules change to use goods-specific criteria values

## Validation Rules

- only WMS rows whose ERP code exists in registered goods are considered
- on-sale filtered aggregation includes rows only when `validPercent != null` and `validPercent >= goods.safetyRate`
- sold-out transition occurs when aggregated qty is less than or equal to `goods.availableQty`
- on-sale transition occurs when filtered aggregated qty is greater than or equal to `goods.availableQty`

## Edge Cases

- no registered goods match for a WMS row
- `validPercent` is null for a candidate row
- multiple lots for the same ERP code where only some rows meet the goods safety-rate threshold
- non-normal goods or excluded/non-numeric/OA110-mismatched zones still skip transition

## Exception Scenarios

- if on-sale checks keep using fixed `50`, per-goods `safetyRate` edits still have no effect
- if sold-out checks reuse the safety-rate-filtered aggregation, low-valid inventory can incorrectly trigger sold-out transitions
- if criteria logs keep using common-config thresholds, the audit trail no longer matches the actual judgment rule

## Acceptance Criteria Mapping

- `판매 상태 체크 기준 데이터 변경`:
  - satisfied by using `goods.availableQty` and `goods.safetyRate` in refinement and policy decisions
- `판매 상태 검증 로직 개선`:
  - satisfied by splitting sold-out and on-sale refinement paths and removing fixed/common thresholds
- `품목 정보로 데이터 검증 후 상태 변경되면 통과`:
  - verified by sync and batch tests that transition based on goods-specific criteria
- `279110073 으로 변경된 로직 검증 후 확인`:
  - supported by the implementation using the goods record for the matched ERP code rather than any fixed threshold
