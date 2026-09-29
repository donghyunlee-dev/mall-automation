# Iteration 32 Result

## Delivered Scope

- Implemented ATCS-226 sold-out stock refinement safety-rate filtering.
- Updated batch execution to pass registered goods data into sold-out refinement.
- Added regression coverage for sold-out safety-rate filtering and null valid-percent exclusion.

## Key Decisions

- Reused one safety-rate candidate rule for both sold-out and on-sale refinement to keep behavior consistent.
- Kept the existing ERP-code set based `refine` overload to avoid breaking existing call sites.
- Did not change API contracts, database schema, Godo payloads, or mail behavior.

## Validation

- PASS: `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
- FAIL: `./gradlew test`

## Open Gaps

- Full test suite has an unrelated failure in `BatchResultMailSenderTest.should_send_batch_result_mail_with_table_body()`: expected `근거 재고수량`, generated `재고수량`.
