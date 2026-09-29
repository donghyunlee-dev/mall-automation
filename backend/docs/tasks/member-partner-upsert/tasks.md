# member-partner-upsert Tasks

## Implementation Tasks

- [x] Update product API documentation for partner upsert behavior.
- [x] Update member partner save use case to insert missing partner rows.
- [x] Preserve existing update behavior and duplicate code validation.

## Test Tasks

- [x] Update integration tests for insert-on-missing behavior.
- [x] Update controller tests to remove update-only 404 expectation.
- [x] Run focused validation commands.

## Documentation Tasks

- [x] Record validation results in `test-result.md`.
- [x] Record delivered scope and gaps in `result.md`.

## Progress Notes

- Started from latest `develop` on branch `feature/member-partner-upsert`.
- Updated upload flow so partner rows are not created by LIST upload or batch-originated normal member data.
- Preserved TEMPLATE upload partner row creation/update as an exception.
- Updated partner save flow so missing partner rows are inserted for existing members.

## Review Notes

- Full test suite passed.
