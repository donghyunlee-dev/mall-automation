# ATCS-234 Spec

## Problem Statement

- Manual force-sync for Godo sold-out status can still fail with a generic `500` when the Godo transport layer raises a non-response client exception.
- The endpoint should keep item-level failure handling and not leak infrastructure exceptions to the caller.

## Business Context

- Operators use the force-sync endpoint to correct a single or multiple goods status values during operations.
- A generic `500` makes the operation look like a backend crash instead of a recoverable Godo API failure.

## Scope

- Map Godo transport client failures in the sold-out sync caller to `GodoApiException`.
- Keep the existing force-sync use case behavior that turns `GodoApiException` into an item-level `FAILED` result.
- Add regression coverage for transport-level failures.

## Out Of Scope

- Changing request validation.
- Changing force-sync eligibility rules.
- Changing the response payload shape.
- Adding new retry policy or new external API behavior.

## User Or Operator Scenarios

- Scenario 1: An operator force-syncs sold-out status and the Godo transport fails to connect. The request should return item-level failure instead of a generic `500`.
- Scenario 2: An operator force-syncs sold-out status and Godo returns a non-success response. The existing failed-item behavior should remain unchanged.

## Acceptance Criteria

- AC1: Transport-layer failures in the sold-out caller no longer surface as unhandled exceptions.
- AC2: Force-sync requests continue returning item-level `FAILED` results for Godo-side failures.
- AC3: Regression tests cover the transport failure path.

## Edge Cases

- Edge case 1: Godo responds with 429 twice and the retry path still fails.
- Edge case 2: Godo transport throws a network-level `RestClientException`.

## Assumptions

- The existing force-sync endpoint should preserve item-level failure semantics rather than expose infrastructure exceptions.

## Clarifications

- Question: Should transport-level Godo failures still be represented as a `FAILED` item?
  Answer: Yes. The endpoint should not leak a generic `500` for recoverable Godo transport failures.
