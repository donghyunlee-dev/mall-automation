# ATCS-235 Spec

## Problem Statement

- Manual force-sync failures are hard to diagnose in production because the backend does not leave enough request-level and Godo-caller-level trace output.
- Operators need to know whether a failure came from goods lookup, Godo response handling, transport failure, or local persistence after the call.

## Business Context

- The force-sync endpoint is used during operations to manually correct sold-out state.
- When it fails only in production, logs are the fastest way to tell whether the backend request reached Godo, received a response, or failed after the external side effect.

## Scope

- Add targeted logs for the manual force-sync request path.
- Add targeted logs for the Godo sold-out API caller path.
- Keep the endpoint behavior unchanged.

## Out Of Scope

- Changing response contracts.
- Changing eligibility rules.
- Changing retry behavior or external API behavior.
- Adding new persistence or schema changes.

## User Or Operator Scenarios

- Scenario 1: An operator sends a force-sync request and the backend logs show the item-level path and final status.
- Scenario 2: A Godo transport failure occurs and the backend logs show the exact failure stage before returning a handled error.

## Acceptance Criteria

- AC1: Force-sync requests produce traceable logs for request start, per-item outcome, and unexpected failure.
- AC2: Godo sold-out API calls produce logs that distinguish response failure, parse failure, and transport failure.
- AC3: Existing API behavior remains unchanged.

## Edge Cases

- Edge case 1: One item in a multi-item request fails while others succeed.
- Edge case 2: Godo returns a non-success response after the backend has already sent the request.

## Assumptions

- Logging should be detailed enough for production diagnosis but should avoid introducing sensitive secrets into logs.

## Clarifications

- Question: Should the fix alter API behavior?
  Answer: No. This task is logging-only and keeps the current endpoint behavior unchanged.
