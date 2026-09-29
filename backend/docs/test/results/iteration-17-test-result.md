# Iteration 17 Test Result

## Executed Commands

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockClientTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest --tests com.sfood.automation.common.exception.GlobalExceptionHandlerTest`
- `./gradlew test`
- `./gradlew build`
- `curl -i http://localhost:8080/api/auth/session`
- `curl -i -X POST http://localhost:8080/api/auth/email-verifications -H 'Content-Type: application/json' -d '{"email":"user@s-food.com"}'`
- direct WMS login request to `https://wms.s-food.com/sessionService/login?...`
- direct WMS stock request to `https://wms.s-food.com/invnTotListService/searchAllStock?...` with the JSON `DS_SEARCHCONDITION` payload from the SRS
- protected API checks after seeding a verification row in `data/sfood-mall-automation.db`

## Results

- targeted regression tests - PASS
- `./gradlew test` - PASS
- `./gradlew build` - PASS
- `GET /api/auth/session` without session - `401` with JSON `AUTHENTICATION_REQUIRED`
- `POST /api/auth/email-verifications` for a valid SFOOD address - `502` with JSON `OTP_MAIL_DELIVERY_FAILED`
- direct WMS login request - PASS, `ErrorCode=0`, `Set-Cookie` present
- direct WMS stock request - PASS, `ErrorCode=0`, `DS_TOTALCOUNT=5230`
- `POST /api/auth/email-verifications/confirm` with a seeded verification row - PASS
- `GET /api/auth/session` with a confirmed session - PASS
- `GET /api/goods` with a confirmed session - PASS
- `GET /api/config/status?scope=GENERAL` with a confirmed session - PASS
- `GET /api/admin/batch-schedules/product-sync` with a confirmed session - PASS
- `POST /api/admin/batch-schedules/product-sync/run` with a confirmed session - PASS, `{"triggered":true,"status":"STARTED"}`

## Notes

- OTP request failures are no longer returned as an opaque `500`; the server logs now preserve the SMTP failure stack trace and the API returns an explicit `502`.
- WMS request formatting now matches the SRS and analysis workbook structure:
  - query string includes `CURRENT_MENUCODE=WMS141123`, `CURRENT_PAGE=1`, `SES_LANG=KO`, `SES_USERGROUP=Observer`, `SES_WHSE=SLP001`, `SES_MULTI_LANG_YN=N`
  - request body sends JSON `DS_SEARCHCONDITION` with `WH_CD=SLP001` and `LOT_ATTR_1=OA110`
- manual batch executions against the shared SQLite DB still showed intermittent failure rows in `batch_execution_log`, including `WMS scraping error` and one earlier `SQLITE_BUSY_SNAPSHOT` row; this remains a runtime risk even after the request-format fix.
