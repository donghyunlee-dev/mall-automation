# ATCS-233 Plan

## Summary

- Prevent hidden goods from being re-exposed by the product sync batch ERP verification path.
- Keep the existing list query behavior that excludes hidden goods unchanged.

## Impacted Modules

- `src/main/java/com/sfood/automation/goods/infrastructure/persistence/`
- `src/main/java/com/sfood/automation/batch/infrastructure/job/`
- `src/test/java/com/sfood/automation/goods/infrastructure/persistence/`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/`

## Architecture Approach

- Keep the fix inside the existing repository and batch orchestration boundaries.
- Exclude `HIDE` goods from ERP verification candidate queries.
- Add a defensive batch-side guard so hidden goods are not restored even if a hidden row reaches the verification result path.

## Data And API Impact

- No schema change.
- No API contract change.
- Behavior change is limited to preserving `HIDE` status during batch ERP verification.

## Validation Plan

- Add repository coverage proving `findAllErpSyncNGoods()` excludes hidden goods.
- Add batch job coverage proving ERP verification does not save or restore hidden goods.
- Run targeted tests for the changed repository and batch job behavior.

## Risks

- Risk 1: A hidden good with `erpSyncYn=N` may currently rely on batch recovery in unintended operational flows.
- Risk 2: Mock-based batch tests may miss persistence filtering unless repository coverage is added separately.

## Rollback Considerations

- Revert the repository filter and batch guard if the change causes an unintended operational regression.
- No data rollback is required because the change only affects runtime selection and state transitions.
