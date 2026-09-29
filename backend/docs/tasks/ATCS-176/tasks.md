# ATCS-176 Tasks

## Development Tasks

- split WMS refinement into sold-out and on-sale candidate aggregation paths
- use per-goods `availableQty` and `safetyRate` as the source of truth for transition criteria
- update the sync use case to consume separate aggregated maps for sold-out and on-sale checks
- cover the new behavior with focused unit tests and batch orchestration tests
- document the implementation and validation outcomes for this Jira task

## Test Cases To Implement

- exclude unregistered ERP codes from both refinement paths
- exclude on-sale candidate rows whose `validPercent` is below the matched goods `safetyRate`
- set sold-out when total aggregated qty is less than or equal to `goods.availableQty`
- set on-sale when safety-rate-filtered aggregated qty is greater than or equal to `goods.availableQty`

## Modules To Modify

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping`
- `src/main/java/com/sfood/automation/goods/domain/policy`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/main/java/com/sfood/automation/batch/infrastructure/job`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping`
- `src/test/java/com/sfood/automation/goods/domain/policy`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/batch/infrastructure/job`
- `docs/tasks/ATCS-176`

## New Classes Or Components

- none

## Integration Points

- `StockDataRefiner`
- `GoodsSoldOutPolicy`
- `SyncGodoStatusUseCase`
- `ProductSyncBatchJob`

## Result

### Changed Files

- pending

### Summary

- pending

### Test Results

- pending

### Review Notes

- pending
