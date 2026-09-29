# ATCS-226 Plan

## Requirement Summary

- Apply each goods item's `safetyRate` to sold-out WMS stock refinement.
- Exclude WMS rows whose `VALID_PERCENT` is null or below the goods `safetyRate` before summing available stock for sold-out decisions.
- Preserve the existing on-sale refinement behavior.

## Business Context

Batch sold-out decisions must use only WMS stock rows that satisfy the remaining shelf-life criteria. Without this, low-validity stock can keep goods on sale even though the stock should not count as available for mall sales.

## Scope

- Change the sold-out refinement path to receive `goodsByErpCode`.
- Apply the same per-goods valid-percent candidate rule used by on-sale refinement to sold-out refinement.
- Update the batch job call site and focused tests.

## Out Of Scope

- No public API changes.
- No schema or migration changes.
- No change to Godo API request/response handling.
- No change to blocked goods, status, zone, or OA110 filtering rules.

## Impacted Modules

- `wms.infrastructure.scraping.StockDataRefiner`
- `batch.infrastructure.job.ProductSyncBatchJob`
- WMS refinement and batch job tests

## Architecture Considerations

- Keep WMS row filtering inside `StockDataRefiner`.
- Keep state transition decisions inside `GoodsSoldOutPolicy`.
- Avoid adding new abstractions because the change is a focused refinement rule update.

## Risk Analysis

- Sold-out transitions may increase because low-validity stock is no longer counted as available.
- Existing tests that assume all registered stock counts for sold-out must be updated to the new requirement.
- On-sale behavior must remain unchanged.
