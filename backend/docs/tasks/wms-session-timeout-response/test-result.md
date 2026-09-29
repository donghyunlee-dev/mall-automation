# WMS Session Timeout Response Test Result

## Scope

- WMS login cookie selection.
- WMS stock response error-code parsing.
- Regression coverage for the observed production error payload.

## Environment

- Profile: test
- Database: not used by focused tests
- External dependencies: mocked transport for automated tests

## Commands

- `cd backend && ./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.WmsLoginClientTest --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockClientTest`
- `cd backend && ./gradlew test`

## Results

- PASS: `WmsLoginClientTest`
- PASS: `WmsStockClientTest`
- PASS: focused Gradle test command completed with `BUILD SUCCESSFUL`
- PASS: full backend Gradle test command completed with `BUILD SUCCESSFUL`

## Failure Notes

- No test failures.
- Gradle emitted existing `MockBean` deprecation warnings from unrelated tests during test compilation.

## Retest Results

- Retest after implementation passed.

## Acceptance Criteria Coverage

- AC1: Covered by `should_prefer_jsessionid_when_login_returns_multiple_cookies`.
- AC2: Covered by `should_throw_wms_error_when_error_code_is_session_timeout_string`.
- AC3: Covered by existing success response tests with numeric zero and missing error semantics in client handling.
- AC4: Covered by numeric non-zero and string `SESSION_TIMEOUT` error tests.
