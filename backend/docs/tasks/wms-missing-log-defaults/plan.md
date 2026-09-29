# WMS Missing Log Defaults Plan

## Summary

- Update the batch status log builder in `SyncGodoStatusUseCase` so missing WMS data writes `0` for both `valid_percent` and `remain_valid_days`.
- Add regression coverage that verifies both values on the missing-WMS path.

## Impacted Modules

- `src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`

## Architecture Approach

- Keep the fix at the application boundary where the status log is assembled.
- Preserve the current policy and transition flow.
- Only normalize the log payload when the WMS aggregation is absent.

## Data And API Impact

- No schema change.
- No API contract change.
- The persisted log row will satisfy the runtime non-null constraint for the missing-WMS path.

## Validation Plan

- Run the focused `SyncGodoStatusUseCaseTest`.
- Run the affected test class or the narrowed test selection if needed.

## Risks

- `0` is a sentinel value, so downstream consumers must not misread it as real WMS evidence.
- The change must remain limited to the missing-WMS case and not overwrite real WMS values.

## Rollback Considerations

- If the sentinel causes confusion later, revert this normalization and revisit the log contract or schema.
