# WMS Session Timeout Response Tasks

## Implementation Tasks

- [x] Preserve duplicate WMS response header values.
- [x] Prefer `JSESSIONID` when WMS login returns multiple cookies.
- [x] Parse WMS stock `ErrorCode` as a flexible string value.
- [x] Treat non-zero/non-blank WMS error codes as `WmsScrapingException`.

## Test Tasks

- [x] Add regression test for multi-cookie login response.
- [x] Add regression test for string `SESSION_TIMEOUT` stock response.
- [x] Run focused WMS client tests.

## Documentation Tasks

- [x] Record production symptom, root cause, fix scope, and validation result.

## Progress Notes

- Live login inspection returned `SCOUTER` before `JSESSIONID`.
- Live stock request with `SCOUTER` returned `ErrorCode: SESSION_TIMEOUT`.
- Live stock request with `JSESSIONID` returned stock data.

## Review Notes

- The change is intentionally limited to WMS infrastructure parsing and cookie selection.
