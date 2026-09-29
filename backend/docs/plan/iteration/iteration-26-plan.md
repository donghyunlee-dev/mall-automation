# Iteration 26 Plan

## Scope

- add a persisted `users` profile table for authenticated login accounts
- create login-time user upsert behavior and authenticated user profile read/update APIs
- route batch result mail recipients through opted-in users and improve the HTML mail format
- update API and data documentation for the new user profile model

## Validation Plan

- `./gradlew test --tests com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCaseTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest`
- `./gradlew test`
