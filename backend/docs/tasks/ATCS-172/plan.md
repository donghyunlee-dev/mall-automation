# ATCS-172 Plan

## Requirement Summary

- fix backend CORS handling so the frontend at `http://localhost:5173` can call backend APIs without browser CORS failures
- allow the API methods used by the existing controllers
- preserve the current session-based authentication flow used by the frontend

## Business Context

- the Jira ticket reports that browser requests from the local frontend cannot reach the backend because CORS blocks them before the API response is usable
- the frontend already uses cookie-based session auth, so the backend must explicitly allow the frontend origin and credentialed requests
- the fix should unblock local frontend development without changing unrelated API behavior

## Scope

- update the shared Spring MVC CORS configuration under `WebMvcConfig`
- allow the local frontend origin `http://localhost:5173`
- allow credentialed browser requests and the HTTP methods already used by the backend APIs
- add focused MVC tests that verify the expected CORS response headers
- document the implementation and validation outcomes for this Jira task

## Out Of Scope

- frontend code changes
- environment/profile-specific CORS policy management beyond the Jira-requested local frontend origin
- authentication, session, or business-logic changes unrelated to CORS
- git branch, commit, push, or pull request automation

## Impacted Modules

- `common.config`
- focused MVC controller tests
- Jira task documentation under `backend/docs/tasks/ATCS-172`

## Architecture Considerations

- keep the change inside `WebMvcConfig`, which already owns global MVC and CORS behavior
- align the CORS policy with the existing session-cookie authentication model by enabling credentials
- avoid introducing new configuration layers unless the ticket requires them

## Risk Analysis

- allowing credentials with a wildcard origin is invalid, so the allowed origin must be explicit
- CORS must continue to permit the existing controller methods, including preflight handling for non-simple requests
- changing global CORS behavior can affect every endpoint, so focused MVC tests are required to guard against regressions
