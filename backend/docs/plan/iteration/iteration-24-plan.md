# Iteration 24 Plan

## Scope

- change OTP email delivery from plain text to structured HTML MIME mail
- keep the existing OTP request flow and delivery failure behavior unchanged
- add focused tests for HTML OTP mail sending and test-double compatibility

## Validation Plan

- `./gradlew test --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest`
