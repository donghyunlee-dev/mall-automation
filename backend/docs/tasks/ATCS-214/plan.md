# ATCS-214 Plan

## Requirement Summary

- fix the batch zone-code judgment so excluded zone rows do not cause valid numeric-zone stock to be discarded for the same ERP code
- apply zone filtering before ERP-code aggregation when evaluating status transitions

## Business Context

- mixed WMS zone rows currently block legitimate on-sale recovery for goods such as ERP code `279110073`
- the batch should decide stock status from usable inventory rows only, not from excluded zone noise

## Scope

- adjust WMS stock refinement to discard invalid zone rows before aggregation
- keep on-sale safety-rate filtering intact after the zone filter
- add focused regression tests for mixed numeric and excluded zone rows
- document the task and validation results

## Out Of Scope

- changes to WMS transport, login, or payload structure
- changes to sold-out/on-sale threshold values
- changes to Godo API integration or scheduler behavior

## Impacted Modules

- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`
- iteration and task result documents

## Architecture Considerations

- keep zone filtering in the WMS refinement layer so policy evaluation still receives already-refined stock facts
- preserve the existing `AggregatedStock` contract and downstream `GoodsSoldOutPolicy` behavior

## Risk Analysis

- filtering too broadly could remove rows that should still contribute to sold-out detection
- changing refinement order could alter existing aggregation expectations, so mixed-zone regression coverage is required
