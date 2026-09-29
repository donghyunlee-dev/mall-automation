# ATCS-209 Tasks

## Development Tasks

- add focused failing tests that lock the OTP mail sender to MIME HTML delivery
- update the OTP mail sender to generate a structured Korean HTML body
- adjust any existing OTP mail recording test doubles so integration coverage still observes sent subjects
- record the scoped Jira delivery and validation results in the task and iteration docs

## Test Cases To Implement

- OTP sender uses `MimeMessage` HTML delivery instead of `SimpleMailMessage`
- OTP sender renders the 6-digit code inside the HTML body
- OTP sender still notifies Slack and throws when MIME delivery fails

## Modules To Modify

- `src/main/java/com/sfood/automation/auth/infrastructure/mail/OtpMailSender.java`
- `src/test/java/com/sfood/automation/auth/infrastructure/mail/OtpMailSenderTest.java`
- `src/test/java/com/sfood/automation/auth/presentation/Task09EndToEndIntegrationTest.java`
- `docs/tasks/ATCS-209/plan.md`
- `docs/tasks/ATCS-209/spec.md`
- `docs/tasks/ATCS-209/tasks.md`
- `docs/plan/iteration/iteration-24-plan.md`
- `docs/test/results/iteration-24-test-result.md`
- `docs/reports/iteration-24-result.md`

## New Classes Or Components

- none

## Integration Points

- `EmailVerificationController -> RequestEmailVerificationUseCase -> OtpMailPort -> OtpMailSender`

## Result

### Changed Files

- `docs/tasks/ATCS-209/plan.md`
- `docs/tasks/ATCS-209/spec.md`
- `docs/tasks/ATCS-209/tasks.md`
- `docs/plan/iteration/iteration-24-plan.md`
- `docs/test/results/iteration-24-test-result.md`
- `docs/reports/iteration-24-result.md`
- `src/main/java/com/sfood/automation/auth/infrastructure/mail/OtpMailSender.java`
- `src/test/java/com/sfood/automation/auth/infrastructure/mail/OtpMailSenderTest.java`
- `src/test/java/com/sfood/automation/auth/presentation/Task09EndToEndIntegrationTest.java`

### Summary

- replaced plain-text OTP delivery with HTML MIME delivery using the existing `JavaMailSender`
- rendered the OTP code as a visually prominent Korean email body without changing the OTP request API
- updated focused unit and end-to-end mail test coverage so MIME-based sending remains observable in tests

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest`
- failed: `./gradlew build`
  - existing failures in `SyncGodoStatusUseCaseTest`
- failed: `./gradlew test`
  - concurrent run with `./gradlew build` produced XML result write conflicts
  - the same run also surfaced the existing `SyncGodoStatusUseCaseTest` failures

### Review Notes

- no prior local task artifacts or matching GitHub PRs were found for `ATCS-209`
- the code change stays inside the OTP mail adapter and the test doubles that observe sent mail
- batch result mail formatting was intentionally left unchanged because it is outside this Jira scope
