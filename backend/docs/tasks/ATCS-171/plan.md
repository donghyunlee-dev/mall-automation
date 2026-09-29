# ATCS-171 Plan

## Requirement Summary

- add a backend logout API so an authenticated user can end the current session explicitly
- make post-logout requests fail authentication instead of continuing to use the prior session
- keep the existing OTP login flow and session issuance behavior unchanged

## Business Context

- the Jira ticket notes that the current one-day session can leave the web page exposed to other people unless the user can terminate the session
- logout must reduce that exposure without changing unrelated authentication or goods-management behavior
- the result should let the frontend force re-login by invalidating the active backend session

## Scope

- expose an authenticated logout endpoint under the existing auth controller
- invalidate the current HTTP session through the existing session port
- handle invalidated session checks safely so auth endpoints return `401 Unauthorized` after logout
- add focused controller and integration coverage for logout and access restriction behavior
- document the implementation and validation outcomes for this Jira task

## Out Of Scope

- OTP request or confirmation rule changes
- frontend logout UI work
- session timeout policy changes
- git branch, commit, push, or pull request automation

## Impacted Modules

- `auth.presentation`
- `auth.application.port`
- `auth.infrastructure.session`
- auth and session-related tests
- Jira task documentation under `backend/docs/tasks/ATCS-171`

## Architecture Considerations

- reuse the existing `AuthSessionPort` abstraction instead of invalidating sessions directly across multiple controllers
- keep logout orchestration in the auth presentation layer because no additional domain rule is introduced by the ticket
- preserve the current `SessionAuthInterceptor` gate so logout remains an authenticated operation

## Risk Analysis

- `/api/auth/session` currently bypasses the interceptor, so invalidated sessions must be handled explicitly to avoid a server error after logout
- logout must not weaken the existing protected-route checks for other APIs
- integration coverage is needed to prove the same session cannot access protected endpoints after invalidation
