# ATCS-209 Spec

## Feature Specification

- OTP email delivery must send a structured HTML message instead of a plain-text body
- the HTML body must present the 6-digit OTP code prominently and use Korean copy
- the email subject remains the OTP verification subject unless implementation constraints require a scoped wording change inside this task

## API Changes

- none

## Database Changes

- none

## Domain Model Changes

- none

## Validation Rules

- OTP mail sending must still use the configured sender account
- the same recipient email passed to the use case must remain the MIME recipient
- SMTP delivery failures must still notify Slack and raise `OtpMailDeliveryException`

## Edge Cases

- HTML content must not expose raw tags in the delivered body
- the MIME message should remain readable even if the recipient client strips some styling
- focused tests must still pass when mail is recorded through a test double rather than a real SMTP server

## Exception Scenarios

- `JavaMailSender` send failures still produce `OTP_MAIL_DELIVERY_FAILED`
- invalid email domain and OTP rate limits remain outside this task and keep current behavior

## Acceptance Criteria Mapping

- AC: OTP email is delivered with a clean structured format
  - send the OTP using an HTML MIME message with visible structure and Korean copy
- AC: the 6-digit value is easy to recognize
  - render the OTP code as the dominant visual element in the body
- AC: plain text only delivery is considered failed for this task
  - replace the plain-text sender path and add focused tests that assert MIME HTML usage
- AC: broken tags or encoding issues are considered failed
  - create the MIME message with explicit HTML mode and UTF-8 charset
