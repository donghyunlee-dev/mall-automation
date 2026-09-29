# ATCS-226 Spec

## Feature Specification

- Sold-out refinement aggregates only WMS rows that match registered goods and satisfy the matched goods item's `safetyRate`.
- A WMS row qualifies for sold-out aggregation when:
  - the ERP code exists in `goodsByErpCode`
  - the zone is numeric and not excluded
  - `validPercent` is not null
  - `validPercent >= goods.safetyRate`
- On-sale refinement keeps its existing rule.

## API Changes

- No public API changes.

## Database Changes

- No schema changes.
- No data migration changes.

## Domain Model Changes

- No domain model field changes.
- Existing `Goods.availableQty` and `Goods.safetyRate` remain the batch criteria source.

## Validation Rules

- Registered goods are matched by ERP code.
- Rows below `goods.safetyRate` are excluded before quantity aggregation.
- Rows with null `validPercent` are excluded before quantity aggregation.
- Existing zone exclusion and numeric-zone checks remain in force.

## Edge Cases

- Multiple rows for the same ERP code where only some rows satisfy `safetyRate`.
- All rows for a goods item fail `safetyRate`, resulting in missing refined stock for that ERP code.
- Unregistered ERP codes remain excluded.
- Excluded or non-numeric zones remain excluded.

## Exception Scenarios

- If sold-out refinement continues using only all ERP codes, low-validity WMS stock can incorrectly prevent sold-out transition.
- If the batch job still calls the old refinement signature, sold-out and on-sale paths will apply different safety-rate semantics.

## Acceptance Criteria Mapping

- Happy path: rows meeting `VALID_PERCENT >= goods.safetyRate` are counted for sold-out judgment.
- Mixed rows: only qualifying rows are summed before sold-out judgment.
- Failure case: null or low `VALID_PERCENT` rows are excluded.
- Existing exclusions: unregistered ERP codes and invalid zones continue to be excluded.
