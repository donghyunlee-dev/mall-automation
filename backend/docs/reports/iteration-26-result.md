# Iteration 26 Result

## Delivered Scope

- added a persisted `users` table and auth-domain repository/model support for authenticated login accounts
- updated OTP confirmation to create or refresh the authenticated user profile before issuing the session
- added authenticated current-user profile read and notification update APIs
- switched batch result emails to opted-in user recipients and rendered the content as structured HTML
- localized batch mail status labels so review recipients see `판매중` and `품절` instead of enum codes
- updated product API/data specs and Jira task artifacts for ATCS-212

## Key Decisions

- kept user profile persistence inside the auth domain and reused the existing auth controller instead of introducing a separate module
- made batch email delivery skip cleanly when no users opted in instead of falling back to the sender account
- kept Teams support as a stored preference only; actual Teams delivery remains out of scope

## Validation

- `./gradlew test --tests com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCaseTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest --tests com.sfood.automation.auth.infrastructure.persistence.UserAccountRepositoryImplTest`
- `./gradlew test --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest --tests com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCaseTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest --tests com.sfood.automation.auth.infrastructure.persistence.UserAccountRepositoryImplTest`
- `./gradlew test`
- `./gradlew test --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest`

## Next Steps

- implement actual Teams notification delivery when the backend integration contract is defined
- add frontend screens for current-user notification preference management
