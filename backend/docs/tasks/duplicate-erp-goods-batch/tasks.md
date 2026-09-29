# duplicate-erp-goods-batch Tasks

## Implementation Tasks

- [x] Remove ERP unique persistence constraints and matching application-level duplicate checks.
- [x] Refactor batch refinement so duplicate ERP goods are evaluated per goods row using shared WMS rows.
- [x] Preserve the existing goods-centric Godo sync behavior for non-duplicate cases.

## Test Tasks

- [x] Add duplicate ERP registration and update tests.
- [x] Add refiner and batch/sync tests covering duplicate ERP goods with different `safetyRate` and `availableQty`.
- [x] Run targeted tests, then the full test suite.
- [x] Run a controlled local or live WMS verification if the environment permits.

## Documentation Tasks

- [x] Record validation commands and outcomes.
- [x] Summarize final behavior, risks, and any unverified gap.

## Progress Notes

- Scope stayed limited to duplicate ERP goods support and related batch evaluation correctness.
- Batch evaluation now keeps WMS stock shared by ERP code while producing goods-id keyed filtered stock results.
- The existing manual force-sync use case was aligned with its documented eligibility rules because the full suite exposed a mismatch.

## Review Notes

- The critical invariant is that WMS rows stay shared by ERP code while filtered stock results become goods-specific.
- The live verification stayed on the WMS fetch/refine path only; live Godo status updates were not executed.
