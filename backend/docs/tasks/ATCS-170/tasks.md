# ATCS-170 Tasks

## Development Tasks

- aggregate registered WMS stock rows into item-level refined stock results
- pass refined aggregated stock into the goods sync flow
- align batch summary reporting and tests with the aggregated refinement result
- document the implementation and validation outcomes for this Jira task

## Test Cases To Implement

- aggregate multiple lot rows for the same registered ERP item into one refined result
- exclude unregistered ERP item codes before aggregation
- allow the sync use case to consume aggregated stock directly without rebuilding the raw refinement step
- keep the batch job summary aligned with aggregated refined stock counts

## Modules To Modify

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/main/java/com/sfood/automation/batch/infrastructure/job`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/batch/infrastructure/job`
- `docs/tasks/ATCS-170`

## New Classes Or Components

- none

## Integration Points

- `ProductSyncBatchJob`
- `StockDataRefiner`
- `SyncGodoStatusUseCase`
- `WmsStockAggregator`
- `GoodsSoldOutPolicy`

## Result

### Changed Files

- `backend/docs/tasks/ATCS-170/plan.md`
- `backend/docs/tasks/ATCS-170/spec.md`
- `backend/docs/tasks/ATCS-170/tasks.md`
- `backend/src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `backend/src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java`
- `backend/src/main/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJob.java`
- `backend/src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`

### Summary

- changed WMS refinement from a raw-row filter into item-level aggregation for registered ERP codes
- passed aggregated refined stock directly from the batch job into the goods sync use case
- updated focused tests and task documentation to reflect the new refinement boundary and batch summary behavior

### Test Results

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest` ✅
- `./gradlew build` ✅
- `./gradlew test` ✅

### Review Notes

- `./gradlew test` was started once in parallel with `./gradlew build`, which caused a transient XML test-report write collision under `build/test-results`; rerunning `./gradlew test` alone succeeded
- git branch, commit, push, and PR creation remain out of scope because `backend/AGENTS.md` requires an explicit user request for repository operations
