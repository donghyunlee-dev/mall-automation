# WMS Session Timeout Response Plan

## Summary

- Update WMS transport response handling so duplicate headers are not collapsed.
- Update login cookie selection to prefer `JSESSIONID`.
- Update stock response parsing so `ErrorCode` can be represented as a string and interpreted safely.
- Add regression tests for the production failure shape.

## Impacted Modules

- `com.sfood.automation.wms.infrastructure.scraping`
- WMS login and stock client tests

## Architecture Approach

- Keep the change inside the WMS infrastructure adapter.
- Preserve the existing `WmsTransport` port shape.
- Extend `WmsTransportResponse` to expose all header values while retaining the existing single-header helper for compatibility.

## Data And API Impact

- No database changes.
- No external API endpoint changes.
- WMS client behavior changes only in cookie selection and error interpretation.

## Validation Plan

- Run focused WMS client tests:
  - `WmsLoginClientTest`
  - `WmsStockClientTest`
- Confirm the observed `SESSION_TIMEOUT` response no longer causes Jackson `int` deserialization failure.
- Optionally run live WMS login/stock inspection separately when operationally needed.

## Risks

- Risk 1: WMS could use another session cookie name in a different environment.
  Mitigation: Fall back to the first parsed cookie when `JSESSIONID` is absent.
- Risk 2: Some WMS responses may omit `ErrorCode`.
  Mitigation: Treat missing or blank error code as success, matching previous `int` default-zero behavior.

## Rollback Considerations

- Revert the WMS client and response DTO changes if WMS requires a different cookie strategy.
- The change is isolated to WMS HTTP parsing and does not require schema rollback.
