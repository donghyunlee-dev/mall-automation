# Iteration 27 Plan

## Scope

- fix WMS stock refinement so excluded or non-numeric zone rows are filtered before ERP-code aggregation
- preserve on-sale safety-rate filtering after the zone filter
- cover the regression with focused refinement and batch orchestration tests for ERP code `279110073`

## Validation Plan

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
