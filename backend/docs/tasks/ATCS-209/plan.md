# ATCS-209 Plan

## Requirement Summary

- change the OTP email body from plain text to a structured HTML format
- make the 6-digit OTP visually prominent and readable in Korean
- keep the existing OTP request flow and failure handling intact

## Business Context

- the Jira ticket reports that the current OTP email is plain text, looks unstructured, and is more likely to be treated as junk mail
- OTP is part of the login flow, so the email body must remain clear without changing the verification contract

## Scope

- replace plain-text OTP body generation with HTML mail generation
- keep the same sender account and the same OTP request API behavior
- update focused tests to verify HTML MIME delivery and content
- update any existing mail test doubles that only capture plain-text messages

## Out Of Scope

- changes to the OTP code generation policy
- changes to allowed recipient domains
- changes to batch result mail formatting
- live production email template hosting or external template engines

## Impacted Modules

- `auth.infrastructure.mail`
- `auth.application.usecase`
- OTP mail sender tests
- OTP end-to-end mail test double
- Jira task and iteration documentation

## Architecture Considerations

- keep `RequestEmailVerificationUseCase -> OtpMailPort -> OtpMailSender` intact
- preserve interface-based wiring and limit the change to the mail adapter layer
- use Spring mail MIME support without introducing a new dependency or template engine

## Risk Analysis

- switching from `SimpleMailMessage` to MIME HTML can break existing tests that only observe plain-text sends
- malformed HTML or incorrect charset handling would directly affect OTP readability
- the change must preserve the current exception path so SMTP failures still surface as `OTP_MAIL_DELIVERY_FAILED`
