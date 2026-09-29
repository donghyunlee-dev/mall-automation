# ATCS-233 Spec

## Problem Statement

- Hidden goods can reappear in the goods list after product sync batch execution.
- The current defect happens when a hidden good remains eligible for ERP verification and the batch changes its status back to a visible state.

## Business Context

- Delete operations for Godo-synced goods rely on `HIDE` instead of physical deletion.
- Once a hidden good becomes `NORMAL` or `ERROR` again, operators see a supposedly deleted item in the goods list, which breaks the expected management flow.

## Scope

- Exclude `HIDE` goods from ERP verification candidate lookup.
- Prevent the batch ERP verification step from restoring or reclassifying hidden goods.
- Add regression coverage for the hidden goods scenario represented by `1000000504`.

## Out Of Scope

- Changing manual register behavior that intentionally restores hidden goods.
- Changing sold-out sync logic for normal goods.
- Changing database schema or external API interactions.

## User Or Operator Scenarios

- Scenario 1: An operator hides a Godo-synced good, runs the product sync batch, and expects the good to remain hidden.
- Scenario 2: A hidden good has `erpSyncYn=N`, but the batch must not convert it to `NORMAL` or `ERROR`.

## Acceptance Criteria

- AC1: Hidden goods are excluded from `findAllErpSyncNGoods()`.
- AC2: Product sync batch does not call recovery or save paths that change a hidden good to a visible status.
- AC3: Goods list behavior for hidden goods remains unchanged.

## Edge Cases

- Edge case 1: A hidden good still has `erpSyncYn=N` and the WMS ERP code exists.
- Edge case 2: A hidden good still has `erpSyncYn=N` and the WMS ERP code does not exist.

## Assumptions

- The intended restore path for hidden goods is manual registration, not batch ERP verification.

## Clarifications

- Question: Should hidden goods ever be restored automatically by batch ERP verification?
  Answer: No. This task protects hidden goods from batch-side status restoration.
