# ATCS-236 Tasks

## Implementation Tasks

- [x] Change manual force-sync status log creation to use a non-null `valid_percent`.
- [x] Keep batch logging unchanged.

## Test Tasks

- [x] Update the manual force-sync use-case test to assert a non-null `valid_percent`.
- [x] Re-run the focused goods tests.

## Documentation Tasks

- [x] Record the non-null sentinel choice and validation outcome.

## Progress Notes

- The runtime DB rejects `null` for `goods_status_log.valid_percent` during manual force-sync persistence.
- The manual force-sync log now uses `0` as the non-null sentinel.

## Review Notes

- Keep the fix narrow so it does not alter batch-derived log semantics.
