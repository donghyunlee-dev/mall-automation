# WMS Missing Log Defaults Spec

## Problem Statement

- When the product sync batch runs without WMS stock data for a goods item, the status transition still occurs, but `goods_status_log.valid_percent` can be written as `null`.
- The runtime database rejects `null` for `goods_status_log.valid_percent`, so the batch fails at persistence time.

## Business Context

- Product sync batch uses WMS-derived evidence when available.
- When no WMS record exists for a goods item, the sync still needs to record a status log and continue safely.

## Scope

- Default `valid_percent` and `remain_valid_days` to `0` when the WMS stock data is missing.
- Keep existing batch transition behavior unchanged otherwise.
- Update regression coverage for the log values.

## Out Of Scope

- Changing status transition rules.
- Changing manual force-sync behavior.
- Changing database schema.
- Changing WMS aggregation rules for items that do exist.

## User Or Operator Scenarios

- Scenario 1: A product sync batch processes a goods item without matching WMS data and persists the status log successfully.
- Scenario 2: The batch continues after persisting the log with neutral defaults.

## Acceptance Criteria

- AC1: When `aggregatedStock` is missing, the persisted status log uses `0` for `valid_percent`.
- AC2: When `aggregatedStock` is missing, the persisted status log uses `0` for `remain_valid_days`.
- AC3: Existing batch transition behavior remains unchanged for items that do have WMS data.
- AC4: Regression tests cover the missing-WMS case.

## Edge Cases

- Edge case 1: WMS data exists but `validPercent` is null.
- Edge case 2: WMS data exists but `remainValidDays` is null.
- Edge case 3: The goods item is eligible for status transition even though WMS data is absent.

## Assumptions

- A missing WMS record is represented by `aggregatedStock == null` in the batch use case.
- `0` is a neutral non-null sentinel for missing WMS-derived status log values.

## Clarifications

- Question: Which fields should default to `0` when WMS data is missing?
  Answer: `valid_percent` and `remain_valid_days`.
