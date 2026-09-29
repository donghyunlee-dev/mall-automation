# ATCS-166 Spec

## Feature Specification

- `GET /api/config/status` returns every common status config as a JSON array
- the response includes both configured scopes used by the service: `GENERAL` and `EXCLUSIVE`
- the read API no longer accepts a `scope` query parameter

## API Changes

- `GET /api/config/status` success response changes from a single `CommonStatusConfigResponse` object to `List<CommonStatusConfigResponse>`
- the query string contract is removed for the read API
- `PUT /api/config/status` remains unchanged

## Database Changes

- no schema changes
- existing `common_status_config` seed data remains the source of the list response

## Domain Model Changes

- no domain model changes
- the repository port adds a bulk-read operation for common status configs

## Validation Rules

- the read API returns an array even when only the default seed rows exist
- the list must include both `GENERAL` and `EXCLUSIVE` when seed data is present
- the update API validation rules remain unchanged

## Edge Cases

- an empty persisted config set returns an empty array rather than a single object
- the list order is stable by persisted row order
- extra unexpected query parameters are ignored by Spring MVC and do not change the response shape

## Exception Scenarios

- missing or expired session still returns `401 Unauthorized`
- unexpected repository failures continue to surface through the shared error handler
- the prior `404` single-scope miss scenario is removed because the API no longer looks up one scope at a time

## Acceptance Criteria Mapping

- `조회 결과 [ { } ] 형태의 Json으로 리턴`:
  - verified by controller and use case tests that assert an array response
- `API 호출 시 scope=GENERAL, scope=EXCLUSIVE 2개의 데이터 조회`:
  - verified by deterministic list coverage for both scopes
- `응답 데이터가 [] 리스트가 아닌 경우 실패`:
  - verified by controller assertions on indexed JSON array elements
- `단일 데이터 {} Object 형태로 리턴되면 실패`:
  - addressed by changing the read endpoint contract to array-only output
