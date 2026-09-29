# ATCS-172 Tasks

## Development Tasks

- update the global MVC CORS configuration for the local frontend origin
- allow credentialed cross-origin requests for the existing session-cookie auth flow
- cover the CORS contract with focused MVC tests
- document the implementation and validation outcomes for the Jira task

## Test Cases To Implement

- allow preflight requests from `http://localhost:5173`
- return `Access-Control-Allow-Origin: http://localhost:5173` on allowed requests
- return `Access-Control-Allow-Credentials: true` for the allowed frontend origin
- preserve successful controller behavior while adding the CORS headers

## Modules To Modify

- `src/main/java/com/sfood/automation/common/config`
- `src/test/java/com/sfood/automation/auth/presentation`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/tasks/ATCS-172`

## New Classes Or Components

- none

## Integration Points

- `WebMvcConfig`
- `SessionAuthInterceptor`
- `EmailVerificationController`
- `GoodsController`

## Result

### Changed Files

- `backend/docs/tasks/ATCS-172/plan.md`
- `backend/docs/tasks/ATCS-172/spec.md`
- `backend/docs/tasks/ATCS-172/tasks.md`
- `backend/src/main/java/com/sfood/automation/common/config/WebMvcConfig.java`
- `backend/src/main/java/com/sfood/automation/common/security/SessionAuthInterceptor.java`
- `backend/src/test/java/com/sfood/automation/auth/presentation/EmailVerificationControllerTest.java`
- `backend/src/test/java/com/sfood/automation/goods/presentation/GoodsListControllerTest.java`

### Summary

- allowed the frontend origin `http://localhost:5173` in the shared MVC CORS config
- enabled credentialed CORS responses so the existing session-cookie auth flow can work from the frontend
- allowed unauthenticated `OPTIONS` preflight requests through the session interceptor so browser preflight checks are not blocked before reaching the CORS layer
- added focused MVC coverage for preflight and authenticated cross-origin responses

### Test Results

- `./gradlew test --tests com.sfood.automation.common.security.SessionAuthInterceptorTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.goods.presentation.GoodsListControllerTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- git branch, commit, push, and PR creation remain out of scope because `backend/AGENTS.md` requires an explicit user request for repository operations
- the existing untracked `frontend/.codex` path was left untouched because it is unrelated to `ATCS-172`
