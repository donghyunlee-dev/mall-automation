# ATCS-212 Tasks

## Development Tasks

- add user profile schema, domain, repository port, and persistence adapter
- update OTP confirmation to create or update the authenticated user profile
- add authenticated current-user read and notification preference update APIs
- switch batch result recipient resolution to opted-in users and change the email body to HTML
- update product API/data docs and iteration result docs

## Test Cases To Implement

- successful OTP confirmation creates a new user profile with default notification flags
- successful OTP confirmation updates the existing user's last login timestamp
- authenticated current-user API returns only the logged-in user's profile
- authenticated notification update API persists `emailNotificationYn` and `teamsNotificationYn`
- batch result mail sender skips delivery when no user opted in
- batch result mail sender sends HTML mail to all opted-in users

## Modules To Modify

- `src/main/resources/schema.sql`
- `docs/product/data-spec.md`
- `docs/product/api-spec.md`
- `src/main/java/com/sfood/automation/auth/**`
- `src/main/java/com/sfood/automation/batch/infrastructure/job/BatchResultMailSender.java`
- `src/test/java/com/sfood/automation/auth/**`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/BatchResultMailSenderTest.java`
- `src/test/java/com/sfood/automation/auth/presentation/Task09EndToEndIntegrationTest.java`
- `docs/tasks/ATCS-212/plan.md`
- `docs/tasks/ATCS-212/spec.md`
- `docs/tasks/ATCS-212/tasks.md`
- `docs/plan/iteration/iteration-26-plan.md`
- `docs/test/results/iteration-26-test-result.md`
- `docs/reports/iteration-26-result.md`

## New Classes Or Components

- user profile domain model and repository port
- user profile DTOs and use cases for current-user read/update
- user profile persistence entity, JPA repository, and adapter

## Integration Points

- `EmailVerificationController -> ConfirmEmailVerificationUseCase -> UserRepository`
- `BatchResultMailSender -> UserRepository`

## Result

### Changed Files

- `docs/tasks/ATCS-212/plan.md`
- `docs/tasks/ATCS-212/spec.md`
- `docs/tasks/ATCS-212/tasks.md`
- `docs/plan/iteration/iteration-26-plan.md`
- `docs/test/results/iteration-26-test-result.md`
- `docs/reports/iteration-26-result.md`
- `docs/product/api-spec.md`
- `docs/product/data-spec.md`
- `src/main/resources/schema.sql`
- `src/main/java/com/sfood/automation/auth/domain/model/UserAccount.java`
- `src/main/java/com/sfood/automation/auth/domain/port/UserAccountRepository.java`
- `src/main/java/com/sfood/automation/auth/infrastructure/persistence/UserAccountEntity.java`
- `src/main/java/com/sfood/automation/auth/infrastructure/persistence/UserAccountJpaRepository.java`
- `src/main/java/com/sfood/automation/auth/infrastructure/persistence/UserAccountRepositoryImpl.java`
- `src/main/java/com/sfood/automation/auth/application/dto/CurrentUserResponse.java`
- `src/main/java/com/sfood/automation/auth/application/dto/UpdateUserNotificationsRequest.java`
- `src/main/java/com/sfood/automation/auth/application/usecase/ConfirmEmailVerificationUseCase.java`
- `src/main/java/com/sfood/automation/auth/application/usecase/GetCurrentUserUseCase.java`
- `src/main/java/com/sfood/automation/auth/application/usecase/UpdateUserNotificationsUseCase.java`
- `src/main/java/com/sfood/automation/auth/presentation/EmailVerificationController.java`
- `src/main/java/com/sfood/automation/common/exception/ErrorCode.java`
- `src/main/java/com/sfood/automation/common/exception/UserNotFoundException.java`
- `src/main/java/com/sfood/automation/batch/infrastructure/job/BatchResultMailSender.java`
- `src/test/java/com/sfood/automation/auth/application/usecase/ConfirmEmailVerificationUseCaseTest.java`
- `src/test/java/com/sfood/automation/auth/presentation/EmailVerificationControllerTest.java`
- `src/test/java/com/sfood/automation/auth/presentation/Task09EndToEndIntegrationTest.java`
- `src/test/java/com/sfood/automation/auth/infrastructure/persistence/UserAccountRepositoryImplTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/BatchResultMailSenderTest.java`
- `src/test/java/com/sfood/automation/common/bootstrap/SqliteBootstrapRunnerTest.java`

### Summary

- added persisted authenticated user profiles with notification preferences and login timestamp tracking
- added authenticated current-user read and notification update APIs backed by the new `users` table
- switched batch result mail recipients to opted-in users and rendered the mail body as HTML
- updated API/data docs and iteration artifacts for ATCS-212

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCaseTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest --tests com.sfood.automation.auth.infrastructure.persistence.UserAccountRepositoryImplTest`
- passed: `./gradlew test --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest --tests com.sfood.automation.auth.application.usecase.ConfirmEmailVerificationUseCaseTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest --tests com.sfood.automation.auth.infrastructure.persistence.UserAccountRepositoryImplTest`
- passed: `./gradlew test`
- passed: `./gradlew test --tests com.sfood.automation.batch.infrastructure.job.BatchResultMailSenderTest`

### Review Notes

- `frontend` worktree changes already existed and were left untouched
- git branch/commit/push/PR steps were not executed automatically because the repository rules require explicit user direction for git operations
- follow-up from closed PR `#64`: batch mail HTML now renders `ON_SALE` as `판매중` and `OUT_OF_STOCK` as `품절`
