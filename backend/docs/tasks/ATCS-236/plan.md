# ATCS-236 Plan

## Summary

- Update the manual force-sync log builder to populate `valid_percent` with `0` instead of `null`.
- Adjust the corresponding use-case test to assert the stored log value is non-null.

## Impacted Modules

- `src/main/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCase.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCaseTest.java`

## Architecture Approach

- Keep the fix at the application use-case boundary where the status log is assembled.
- Preserve the current force-sync flow and only change the non-null log value.

## Data And API Impact

- No schema change.
- No API contract change.
- The persisted log row will now satisfy the runtime `NOT NULL` constraint for `valid_percent`.

## Validation Plan

- Run the focused force-sync use case test.
- Run the controller test that exercises the manual force-sync endpoint.

## Risks

- Using `0` may be interpreted as real evidence if not understood by downstream readers.
- The change must stay limited to manual force-sync and not affect batch logs.

## Rollback Considerations

- If downstream consumers dislike the sentinel value, revert the change and revisit the schema or domain contract later.
