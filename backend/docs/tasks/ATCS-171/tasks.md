# ATCS-171 Tasks

## Development Tasks

- add an authenticated logout endpoint to the auth controller
- invalidate the active HTTP session through the existing auth session port
- make session-status reads safe for invalidated sessions
- document the implementation and validation outcomes for the Jira task

## Test Cases To Implement

- return `204 No Content` for logout with an authenticated session
- return `401 Unauthorized` for logout without a session
- return `401 Unauthorized` when `/api/auth/session` is called with an invalidated session
- reject protected APIs after a successful logout using the same session

## Modules To Modify

- `src/main/java/com/sfood/automation/auth/presentation`
- `src/test/java/com/sfood/automation/auth/presentation`
- `docs/tasks/ATCS-171`

## New Classes Or Components

- none

## Integration Points

- `EmailVerificationController`
- `AuthSessionPort`
- `SessionIssuer`
- `SessionAuthInterceptor`

## Result

### Changed Files

- `backend/docs/product/api-spec.md`
- `backend/docs/tasks/ATCS-171/plan.md`
- `backend/docs/tasks/ATCS-171/spec.md`
- `backend/docs/tasks/ATCS-171/tasks.md`
- `backend/src/main/java/com/sfood/automation/auth/presentation/EmailVerificationController.java`
- `backend/src/test/java/com/sfood/automation/auth/presentation/EmailVerificationControllerTest.java`
- `backend/src/test/java/com/sfood/automation/auth/presentation/OtpAuthenticationFlowIntegrationTest.java`

### Summary

- added `POST /api/auth/logout` to invalidate the authenticated HTTP session through the existing session port
- hardened `/api/auth/session` so invalidated sessions return the standard `401 Unauthorized` response instead of surfacing an invalid-session error
- documented the new public auth endpoint and covered the logout flow with controller and integration tests

### Test Results

- `./gradlew test --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.auth.presentation.OtpAuthenticationFlowIntegrationTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- git branch, commit, push, and PR creation remain out of scope because `backend/AGENTS.md` requires an explicit user request for repository operations
- the existing untracked `frontend/.codex` path was left untouched because it is unrelated to `ATCS-171`
