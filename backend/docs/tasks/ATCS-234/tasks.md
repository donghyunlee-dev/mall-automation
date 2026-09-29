# ATCS-234 Tasks

## Implementation Tasks

- [x] Broaden sold-out caller exception handling for transport-level client failures.
- [x] Keep the use case behavior unchanged for `GodoApiException`.

## Test Tasks

- [x] Add regression coverage for transport-level Godo client failure.
- [x] Re-run force-sync use case and controller tests.

## Documentation Tasks

- [x] Record scope and validation results for this task.

## Progress Notes

- Manual force-sync was still leaking a generic `500` when the transport layer failed before a `GodoApiException` was raised.
- The caller now maps transport-level `RestClientException` failures into `GodoApiException`.

## Review Notes

- Keep the fix scoped to the external Godo sold-out caller.
