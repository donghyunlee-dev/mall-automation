# ATCS-234 Plan

## Summary

- Broaden the sold-out API caller exception handling so Godo transport failures are converted into `GodoApiException`.
- Add a regression test that simulates a transport-level client failure and verifies the caller does not leak a generic server error.

## Impacted Modules

- `src/main/java/com/sfood/automation/goods/infrastructure/external/SoldoutStatusApiCaller.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/external/SoldoutStatusApiCallerTest.java`

## Architecture Approach

- Keep the fix in the infrastructure adapter where the transport exception originates.
- Preserve the application layer contract: the use case still catches `GodoApiException` and returns an item-level `FAILED` response.

## Data And API Impact

- No schema impact.
- No response contract changes.
- Existing `/api/goods/godo-status/force-sync` output stays the same.

## Validation Plan

- Run the focused sold-out caller test set.
- Run the force-sync use case and controller tests that cover the manual sync flow.

## Risks

- Catching a broader client exception type could hide a transport bug if not kept narrow enough.
- If another uncaught runtime remains in the caller path, the generic `500` risk would persist.

## Rollback Considerations

- Revert the caller exception handling change and the added regression test if the broader catch introduces unexpected behavior.
