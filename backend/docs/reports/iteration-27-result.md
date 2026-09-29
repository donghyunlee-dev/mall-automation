# Iteration 27 Result

## Delivered Scope

- fixed WMS refinement so excluded and non-numeric zone rows are discarded before ERP-code aggregation
- preserved on-sale safety-rate filtering after the zone filter
- added regression coverage for mixed-zone inventory rows, including ERP code `279110073`

## Key Decisions

- kept zone filtering inside `StockDataRefiner` so downstream policy evaluation continues to consume already-refined stock facts
- left `GoodsSoldOutPolicy` unchanged because the bug was caused by refinement order, not by threshold or transition rules

## Validation

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest`
- `./gradlew test`

## Next Steps

- run a manual product-sync batch against the live process and confirm ERP code `279110073` now evaluates from the remaining `ZONE_CD=43` stock only
