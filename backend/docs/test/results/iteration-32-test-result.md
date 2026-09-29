# Iteration 32 Test Result

## Scope

- ATCS-226: apply goods-specific safety-rate filtering to sold-out WMS stock refinement.

## Commands

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
- `./gradlew test`

## Results

- PASS: targeted ATCS-226 tests completed successfully.
- FAIL: full test suite failed on an existing unrelated mail template assertion.

## Failure Detail

- Test: `BatchResultMailSenderTest.should_send_batch_result_mail_with_table_body()`
- Reason: expected mail HTML to contain `근거 재고수량`, but generated HTML contains `재고수량`.
- ATCS-226 changed stock refinement and batch invocation only; mail template code was not modified.

## Notes

- Sold-out refinement now excludes WMS rows whose `validPercent` is null or below the goods `safetyRate`.
- Sold-out aggregation now matches the on-sale safety-rate filtering behavior.
