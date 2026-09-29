# ATCS-172 Spec

## Feature Specification

- backend responses must allow browser requests from `http://localhost:5173`
- CORS responses must support credentialed requests so the frontend session cookie can be sent
- the configured allowed methods must cover the API methods already exposed by the backend controllers

## API Changes

- no endpoint path or payload changes
- cross-origin responses for allowed requests now include the headers required for `http://localhost:5173`

## Database Changes

- no schema changes
- no data changes

## Domain Model Changes

- no domain model changes
- the fix is confined to shared MVC configuration

## Validation Rules

- requests with `Origin: http://localhost:5173` receive `Access-Control-Allow-Origin: http://localhost:5173`
- CORS responses include `Access-Control-Allow-Credentials: true`
- preflight responses allow the backend HTTP methods in current use: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`

## Edge Cases

- browser preflight for authenticated frontend requests
- cross-origin `GET` requests to session-based endpoints
- non-allowed origins remain outside this Jira scope and are not opened broadly

## Exception Scenarios

- missing CORS origin support continues to block the frontend in the browser
- omitting credential support continues to block cookie-based authentication even if the origin is allowed
- limiting methods too narrowly can still fail preflight checks for valid frontend API calls

## Acceptance Criteria Mapping

- `웹 사이트에서 API 접근 시 허용 설정`:
  - verified by MVC tests that allow `Origin: http://localhost:5173`
- `API 접근 시 정상적인 Response 응답`:
  - verified by asserting the expected CORS headers on preflight and request responses
- `cors 오류 발생`:
  - prevented by explicit origin and credentials support in the shared MVC config
- `cors 설정에 대한 테스트 후 확인`:
  - satisfied by focused controller tests covering the configured CORS behavior
