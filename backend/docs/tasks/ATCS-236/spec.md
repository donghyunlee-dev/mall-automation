# ATCS-236 Spec

## Problem Statement

- Manual force-sync succeeds at the Godo side but fails when persisting `goods_status_log` because `valid_percent` is written as `null`.
- The runtime database enforces `goods_status_log.valid_percent` as `NOT NULL`, so the request fails after the external side effect.

## Business Context

- Manual force-sync is used to correct sold-out state for a specific goods item.
- Operators need the backend to finish the request cleanly after Godo accepts the change.

## Scope

- Fix the force-sync status log creation so `valid_percent` is always non-null.
- Keep the existing manual force-sync behavior unchanged otherwise.
- Update regression coverage for the status log value.

## Out Of Scope

- Changing force-sync eligibility rules.
- Changing Godo request/response handling.
- Changing database schema.
- Changing batch sold-out sync logic.

## User Or Operator Scenarios

- Scenario 1: An operator force-syncs a goods item and the backend persists the status log successfully.
- Scenario 2: A manual force-sync request no longer fails after Godo accepts the change.

## Acceptance Criteria

- AC1: Manual force-sync no longer writes `null` into `goods_status_log.valid_percent`.
- AC2: Manual force-sync continues to return success for a successful Godo response.
- AC3: Regression tests cover the non-null log value.

## Edge Cases

- Edge case 1: The goods item has no WMS-derived valid percent because manual force-sync does not use WMS data.
- Edge case 2: The goods item is already in the target sold-out state.

## Assumptions

- Since manual force-sync does not use WMS evidence, `valid_percent` can use a neutral non-null sentinel value.

## Clarifications

- Question: What should `valid_percent` be for manual force-sync logs?
  Answer: Use `0` as the non-null sentinel value because manual force-sync does not calculate a WMS valid percent.
