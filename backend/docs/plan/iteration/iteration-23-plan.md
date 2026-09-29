# Iteration 23 Plan

## Scope

- replace the temporary Slack-only OTP delivery with real SMTP mail sending through Spring `JavaMailSender`
- align mail configuration keys so Gmail SMTP settings can be supplied directly from `application.yml` or environment variables
- add focused OTP mail sender coverage for successful delivery and failure handling

## Validation Plan

- `./gradlew test --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest`
