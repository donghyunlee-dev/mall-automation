# ATCS-170 Spec

## Feature Specification

- the batch must keep raw WMS rows for ERP verification
- after ERP verification, the refinement step must:
- filter out unregistered ERP item codes
- group the remaining rows by ERP item code
- sum `invnQty` across all rows in the group
- keep the minimum non-null `validPercent` for the group
- preserve whether at least one lot exists for the grouped item
- preserve the zone and lot attributes needed by sold-out policy evaluation
- the goods sync flow must consume the aggregated refined stock directly when evaluating sold-out and on-sale transitions

## API Changes

- none

## Database Changes

- none

## Domain Model Changes

- reuse `AggregatedStock` as the refined stock representation passed from the batch job into the sync use case

## Validation Rules

- only registered ERP item codes participate in refinement
- multiple WMS rows with the same ERP item code must become one refined stock result
- aggregated quantity must equal the sum of all grouped `invnQty` values
- aggregated valid percent must equal the minimum non-null `validPercent` in the group

## Edge Cases

- a single ERP item appears in only one WMS row
- a grouped ERP item has mixed null and non-null `validPercent` values
- an unregistered ERP item appears in raw WMS rows and must be dropped before aggregation
- grouped rows include lot metadata needed for sold-out policy gating

## Exception Scenarios

- raw WMS collection failures remain unchanged and still fail the batch before refinement
- empty refined stock input must result in no goods status transitions

## Acceptance Criteria Mapping

- `WMS 재고 데이터 수집 후 품목별 수량 합계 데이터 생성`
- implemented by item-level aggregation inside `StockDataRefiner`
- `WMS 재고 데이터 정제된 데이터 생성 후 진행`
- implemented by passing aggregated refined stock from `ProductSyncBatchJob` into `SyncGodoStatusUseCase`
- `정제된 데이터 존재하지 않음`
- covered by no-op sync behavior when the refined aggregated stock is empty
- `정제된 데이터 0건`
- covered by refinement tests and batch summary updates that reflect aggregated result counts
