# Iteration 32 Plan

## Scope

- implement ATCS-226
- apply goods-specific `safetyRate` filtering to sold-out WMS stock refinement
- keep on-sale refinement, zone filtering, and Godo sync behavior unchanged
- update focused task and test documentation

## Validation Plan

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
- `./gradlew test`
