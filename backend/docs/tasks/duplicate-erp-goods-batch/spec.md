# duplicate-erp-goods-batch Spec

## Problem Statement

- The current goods model and batch flow assume one `erpGoodsCode` maps to one goods row.
- Operators need to register multiple Godo goods codes against the same ERP goods code and apply different `availableQty` and `safetyRate` criteria per goods item.
- The current batch refinement path collapses ERP-to-goods matching to a single goods item, which makes duplicate ERP goods behave incorrectly.

## Business Context

- A single WMS ERP item can back multiple Godo storefront products with different pricing, promotion scope, and sold-out thresholds.
- WMS stock remains shared, but sold-out judgments must be applied per goods row using each goods item's own criteria.
- This is an operationally sensitive batch path, so changes must preserve current single-goods behavior while extending duplicate-ERP support safely.

## Scope

- Remove the uniqueness assumption for `erpGoodsCode` in persistence and registration/update validation.
- Update batch refinement and sync preparation so duplicate ERP goods use shared WMS rows but evaluate per goods item.
- Add or update tests that cover duplicate ERP goods behavior in registration and batch processing.

## Out Of Scope

- Changing WMS transport, scraping endpoints, or WMS aggregation rules by ERP code.
- Changing Godo API request/response contracts.
- Adding new operator UI behavior or new batch scheduling behavior.
- Deleting or rewriting unrelated task documents.

## User Or Operator Scenarios

- Scenario 1:
  - An operator registers two goods rows with different Godo goods codes and the same ERP goods code.
  - The batch uses the same WMS ERP stock source for both rows, but each row uses its own thresholds when deciding status changes.
- Scenario 2:
  - An operator updates an existing goods row to reuse an ERP goods code already used by another visible goods row.
  - The update succeeds if the Godo goods code remains unique.

## Acceptance Criteria

- AC1:
  - Goods registration and update no longer reject duplicate `erpGoodsCode` values.
- AC2:
  - Batch execution evaluates duplicate ERP goods rows independently and can trigger separate Godo status updates based on each goods row's criteria.
- AC3:
  - A goods row's `safetyRate` filters WMS rows for that goods row only; another goods row sharing the same ERP code can produce a different filtered stock result.
- AC4:
  - Existing single-ERP-goods behavior remains unchanged for non-duplicate cases.

## Edge Cases

- Edge case 1:
  - Two goods rows share one ERP code and one row filters out all WMS rows by `safetyRate` while the other still has eligible stock.
- Edge case 2:
  - Duplicate ERP goods rows use the same thresholds, producing the same stock result and the same status transition outcome.
- Edge case 3:
  - Hidden goods rows must not interfere with visible goods rows when sharing an ERP code.

## Assumptions

- WMS stock rows remain keyed by ERP item code and are shared across Godo goods rows.
- Godo goods code remains unique.
- Per-goods `availableQty` and `safetyRate` are the source of truth for batch judgments.

## Clarifications

- Question:
  Should duplicate ERP goods use one shared WMS source but evaluate sold-out criteria per goods row?
  Answer:
  Yes. Shared WMS stock is correct; sold-out judgment criteria are goods-specific.
