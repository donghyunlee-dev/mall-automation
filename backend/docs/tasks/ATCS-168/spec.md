# ATCS-168 Spec

## Feature Specification

- `WmsLoginClient` must accept the WMS session cookie even when the login response header name differs in case from `Set-Cookie`
- the extracted session cookie continues to be passed unchanged to the stock collection request
- the login flow still fails when no cookie is present or when the cookie cannot be parsed

## API Changes

- no public API changes
- no controller, request, or response contract changes

## Database Changes

- no schema changes
- no data changes

## Domain Model Changes

- no domain model changes
- the fix is confined to WMS scraping infrastructure behavior

## Validation Rules

- login must continue to send the existing query parameters to the WMS login endpoint
- cookie extraction must resolve `Set-Cookie` case-insensitively
- missing cookie headers must continue to raise `WmsScrapingException`

## Edge Cases

- login response uses `set-cookie` or another capitalization variant
- login response includes an empty cookie header value
- login response header exists but contains an unparsable cookie string

## Exception Scenarios

- absent session cookie still throws `WmsScrapingException`
- unparsable cookie values still throw `WmsScrapingException`
- downstream WMS stock parsing behavior remains unchanged

## Acceptance Criteria Mapping

- `Session ID 설정`:
  - verified by login-client tests that recover the session cookie from case-insensitive response headers
- `WMS 재고 데이터 정상 수집`:
  - preserved by keeping the existing cookie handoff contract from login to stock fetch
- `URL 호출 시 오류 발생 / 예외 발생`:
  - guarded by retaining the explicit scraping exception when no usable cookie exists
