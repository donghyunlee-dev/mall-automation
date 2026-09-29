# Iteration 17 Plan

## Scope

- fix WMS stock request formatting to match the SRS and analysis workbook structure
- normalize `GET /api/auth/session` unauthorized responses to the shared `ErrorResponse` format
- convert OTP mail send failures from a generic `500` into an explicit business error response
- verify the affected APIs against the running service and record the results

## Validation Plan

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockClientTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest --tests com.sfood.automation.common.exception.GlobalExceptionHandlerTest`
- `./gradlew test`
- `./gradlew build`
- live API checks against `http://localhost:8080` using `data/sfood-mall-automation.db`
