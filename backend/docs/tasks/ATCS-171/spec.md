# ATCS-171 Spec

## Feature Specification

- `POST /api/auth/logout` invalidates the currently authenticated HTTP session
- a successful logout response returns without a body
- after logout, the same session can no longer access `/api/auth/session` or other protected APIs
- existing OTP login and session issuance remain unchanged

## API Changes

- add `POST /api/auth/logout`
- request body: none
- response: `204 No Content` on success
- unauthorized callers still receive the standard authentication error response from the existing session guard

## Database Changes

- no schema changes
- no data changes

## Domain Model Changes

- no domain model changes
- session invalidation reuses the existing auth session port contract

## Validation Rules

- logout must invalidate the current HTTP session when one exists
- `/api/auth/session` must return `401 Unauthorized` when the session is missing or already invalidated
- protected APIs must continue to reject the invalidated session after logout

## Edge Cases

- `/api/auth/session` receives an already invalidated session object
- the client reuses the same session cookie after logout
- logout is requested without an authenticated session

## Exception Scenarios

- missing session for logout still returns `401 Unauthorized`
- invalidated session reads do not raise an internal server error
- other auth endpoints keep their existing validation and exception behavior

## Acceptance Criteria Mapping

- `로그 아웃 API 기능 구현`:
  - verified by controller coverage for `POST /api/auth/logout`
- `로그 아웃 시 접근 제한 처리`:
  - verified by integration coverage showing the logged-out session receives `401 Unauthorized` on `/api/auth/session` and `/api/goods`
- `세션 정보 삭제 확인`:
  - verified by exercising the same session after logout and observing authentication failure
- `로그아웃 후 정상적인 인증 오류 메시지 확인`:
  - verified by controller and integration assertions on the standard auth error response
