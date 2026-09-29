# WMS Missing Log Defaults Tasks

## Implementation Tasks

- [x] Update batch status log creation to write `0` for `valid_percent` when WMS data is missing.
- [x] Update batch status log creation to write `0` for `remain_valid_days` when WMS data is missing.

## Test Tasks

- [x] Add regression coverage for the missing-WMS status log values.
- [x] Run the focused batch use-case tests.

## Documentation Tasks

- [x] Record the changed behavior and validation outcome.

## Progress Notes

- The batch path currently writes `null` for missing WMS-derived log fields.
- The change should stay limited to the missing-WMS case.
