# ATCS-226 Tasks

## Development Tasks

- Update sold-out stock refinement to use `goodsByErpCode`.
- Apply per-goods valid-percent filtering before sold-out aggregation.
- Update `ProductSyncBatchJob` to call the revised sold-out refinement path.
- Preserve existing on-sale refinement behavior.

## Test Cases To Implement

- Sold-out refinement excludes rows below goods `safetyRate`.
- Sold-out refinement excludes rows with null `validPercent`.
- Batch job passes goods map to the sold-out refinement path.
- Existing on-sale and zone filtering tests remain green.

## Modules To Modify

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/main/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJob.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`

## New Classes Or Components

- None.

## Integration Points

- `ProductSyncBatchJob -> StockDataRefiner -> WmsStockAggregator`
- `SyncGodoStatusUseCase` consumes the refined sold-out and on-sale stock maps.

## Result

### Changed Files

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/main/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJob.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`
- `docs/plan/iteration/iteration-32-plan.md`
- `docs/test/results/iteration-32-test-result.md`
- `docs/reports/iteration-32-result.md`

### Summary

- Added a sold-out refinement path that receives `goodsByErpCode` and filters WMS rows by each goods `safetyRate` before aggregation.
- Updated `ProductSyncBatchJob` to use the goods-aware sold-out refinement path.
- Reused the same safety-rate candidate rule for sold-out and on-sale refinement.
- Preserved the existing registered ERP-code refinement overload for compatibility.

### Test Results

- PASS: `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
- FAIL: `./gradlew test`
- Full test failure is unrelated to ATCS-226: `BatchResultMailSenderTest.should_send_batch_result_mail_with_table_body()` expects `근거 재고수량`, but generated HTML contains `재고수량`.

### Review Notes

- Sold-out and on-sale stock maps now use the same zone and safety-rate eligibility rule.
- No API, database schema, or Godo request contract changes were made.
- Existing full-suite mail template test failure was not fixed because it is outside ATCS-226 scope.
