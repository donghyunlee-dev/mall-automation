# ATCS-214 Tasks

## Development Tasks

- add task and iteration planning artifacts for the zone-code refinement bug
- add regression coverage for mixed numeric and excluded WMS zone rows
- update WMS refinement so zone filtering happens before ERP-code aggregation
- record validation and delivery notes for the bug fix

## Test Cases To Implement

- mixed numeric and excluded zone rows keep the numeric-zone quantity in sold-out refinement
- mixed numeric and excluded zone rows keep only valid-zone quantity in on-sale refinement
- product sync passes the filtered quantity for ERP code `279110073` into the sync use case

## Modules To Modify

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`
- `docs/tasks/ATCS-214/plan.md`
- `docs/tasks/ATCS-214/spec.md`
- `docs/tasks/ATCS-214/tasks.md`
- `docs/plan/iteration/iteration-27-plan.md`
- `docs/test/results/iteration-27-test-result.md`
- `docs/reports/iteration-27-result.md`

## New Classes Or Components

- none

## Integration Points

- `ProductSyncBatchJob -> StockDataRefiner -> SyncGodoStatusUseCase`

## Result

### Changed Files

- `docs/tasks/ATCS-214/plan.md`
- `docs/tasks/ATCS-214/spec.md`
- `docs/tasks/ATCS-214/tasks.md`
- `docs/plan/iteration/iteration-27-plan.md`
- `docs/test/results/iteration-27-test-result.md`
- `docs/reports/iteration-27-result.md`
- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`

### Summary

- filtered invalid and excluded WMS zone rows before ERP-code aggregation so mixed zone codes no longer suppress valid stock
- preserved on-sale safety-rate filtering after the zone filter
- covered the `279110073` regression with focused refinement and batch orchestration tests

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
- passed: `./gradlew test`

### Review Notes

- `frontend` worktree changes already existed and were left untouched
- direct WMS verification showed `279110073` with `ZONE_CD=43, INVN_QTY=38` and `ZONE_CD=SHP, INVN_QTY=40`; the fix keeps the `43` stock and excludes `SHP`
