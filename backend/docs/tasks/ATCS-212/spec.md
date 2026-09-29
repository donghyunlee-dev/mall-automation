# ATCS-212 Spec

## Feature Specification

- successful OTP confirmation must create a `users` row when the authenticated email is new
- successful OTP confirmation must update `last_login_at` when the authenticated email already exists
- new `users` rows must default `email_notification_yn='N'` and `teams_notification_yn='N'`
- authenticated clients must be able to fetch the current user's profile and update the two notification flags
- batch result emails must be sent only to users whose `email_notification_yn='Y'`
- batch result emails must be rendered as HTML with summary content and a goods-change table

## API Changes

- add `GET /api/auth/me`
- add `PUT /api/auth/me/notifications`
- update API documentation for the new user profile endpoints

## Database Changes

- add `users` table with:
  - `id`
  - `email` unique
  - `email_notification_yn`
  - `teams_notification_yn`
  - `last_login_at`
  - `created_at`
  - `updated_at`
- add indexes needed for unique email lookup and notification recipient lookup

## Domain Model Changes

- add a user profile domain model with login timestamp and notification preference mutation rules
- add a repository port for user lookup, save, and opted-in email recipient queries
- update OTP confirmation use case to upsert the user profile before issuing the session

## Validation Rules

- user profile lookup and update are always bound to the authenticated session email
- notification update request values must be limited to `Y` or `N`
- profile APIs must never return another user's data

## Edge Cases

- first login for a new email
- repeat login for an existing email
- batch run with changed logs but no opted-in email recipients
- authenticated profile lookup when a session exists but the user row is unexpectedly missing

## Exception Scenarios

- missing authenticated session still returns `401`
- missing current user profile returns a business error instead of silently fabricating data
- SMTP send failures still record failed send logs and notify Slack

## Acceptance Criteria Mapping

- AC: 신규 계정 로그인 시 users 테이블에 데이터 생성
  - persist a new user profile on the first successful OTP confirmation
- AC: 존재하는 사용자 로그인 시 로그인일자 업데이트
  - update `last_login_at` on repeat successful OTP confirmation
- AC: 웹페이지에서 사용자 조회 API 정보 호출 시 데이터를 응답
  - add authenticated current-user read API
- AC: 이메일/팀즈 알림 설정 변경 후 users 업데이트
  - add authenticated notification preference update API with persisted flags
- AC: 상태 변경 이메일 전송 대상은 opted-in 사용자
  - resolve batch recipients from `users.email_notification_yn='Y'`
- AC: 이메일 내용 형식 개선
  - render batch result email as HTML with structured summary and table rows
