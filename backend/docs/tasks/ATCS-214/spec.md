# ATCS-214 Spec

## Feature Specification

- WMS refinement must remove rows from excluded or non-numeric zones before grouping rows by ERP code
- after zone filtering, remaining rows for the same ERP code must still be aggregated across multiple lots
- on-sale refinement must continue to require `VALID_PERCENT >= goods.safetyRate` in addition to the zone filter

## API Changes

- none

## Database Changes

- none

## Domain Model Changes

- none
- `AggregatedStock` remains the refined-stock contract consumed by the policy and sync use case

## Validation Rules

- keep only rows whose `ZONE_CD` is numeric
- exclude rows whose `ZONE_CD` is one of the blocked zone codes such as `SHP`
- keep only rows whose `LOT_ATTR_1` remains compatible with the existing downstream policy expectations
- on-sale candidates still require a non-null `VALID_PERCENT` meeting the goods safety rate

## Edge Cases

- one ERP code has both numeric-zone and excluded-zone rows
- one ERP code has multiple valid lots after excluded zones are removed
- one ERP code has only excluded-zone rows and must disappear from the refined result

## Exception Scenarios

- no new exception path is introduced; empty refined output should continue to produce no transition

## Acceptance Criteria Mapping

- mixed zone codes are evaluated from the remaining valid rows only
- ERP code `279110073` uses the `ZONE_CD=43` quantity for state judgment instead of being skipped by the `ZONE_CD=SHP` row
- valid lots are still aggregated after the zone filter is applied
