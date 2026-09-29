# WMS Session Timeout Response Spec

## Problem Statement

- WMS stock fetch failed with a Jackson parsing error when the WMS response contained `"ErrorCode":"SESSION_TIMEOUT"`.
- The current response DTO expected `ErrorCode` as an `int`, so the application could not surface the actual WMS session error.
- Live login inspection also showed WMS returns both `SCOUTER` and `JSESSIONID` cookies, and stock requests require `JSESSIONID`.

## Business Context

- The product sync batch depends on WMS stock data.
- When WMS rejects the stock request because the session is invalid, operators need the actual WMS error message instead of a low-level deserialization failure.
- The batch must reuse the authenticated WMS session cookie after login.

## Scope

- Preserve multiple WMS response header values from the transport layer.
- Prefer `JSESSIONID` when selecting the WMS login cookie.
- Allow WMS stock `ErrorCode` to be a string, number, or absent.
- Convert non-zero/non-blank WMS error codes into `WmsScrapingException` with the WMS error message.
- Add focused tests for the observed `SESSION_TIMEOUT` response and multi-cookie login response.

## Out Of Scope

- No change to WMS login URL, stock URL, credentials, or search conditions.
- No change to Godo sold-out status synchronization behavior.
- No schema or public API contract change.

## User Or Operator Scenarios

- Scenario 1: WMS login returns `SCOUTER` and `JSESSIONID`; the batch uses `JSESSIONID` for stock fetch.
- Scenario 2: WMS stock fetch returns `{"ErrorCode":"SESSION_TIMEOUT"}`; the application reports a WMS scraping error instead of a JSON type conversion error.

## Acceptance Criteria

- AC1: WMS login selects `JSESSIONID` when multiple cookies are returned.
- AC2: WMS stock client handles string `ErrorCode` values without deserialization failure.
- AC3: Numeric zero, string zero, blank, or missing `ErrorCode` are treated as non-error responses.
- AC4: Non-zero/non-blank `ErrorCode` values raise `WmsScrapingException` with code and message.

## Edge Cases

- Multiple `Set-Cookie` headers with `SCOUTER` before `JSESSIONID`.
- Lowercase or differently cased header names.
- WMS success response without `ErrorCode`.
- WMS error response without `DS_OUT`.

## Assumptions

- `JSESSIONID` is the WMS authenticated session cookie required by stock APIs.
- `SCOUTER` is not sufficient for authenticated WMS stock requests.

## Clarifications

- Question: Is the observed Jackson error the root failure?
  Answer: It is the direct failure in logs, but it is triggered by a WMS session error response. The fix must both parse the response safely and select the correct session cookie.
