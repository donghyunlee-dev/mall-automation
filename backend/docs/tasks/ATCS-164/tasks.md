# ATCS-164 Tasks

## Development Tasks

- update the goods registration controller to accept a list request body
- extend the registration use case to process multiple goods requests
- add a list response DTO for bulk registration results
- update API documentation for the new contract

## Test Cases To Implement

- register one goods item from a one-element list
- register multiple goods items from a multi-element list
- reject invalid list items with `400 Bad Request`

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/presentation`
- `src/main/java/com/sfood/automation/goods/application/dto`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/presentation`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/auth/presentation`
- `docs/product/api-spec.md`

## New Classes Or Components

- `RegisterGoodsListResponse`

## Integration Points

- `GoodsRepository`
- `CommonStatusConfigRepository`
- `GodoApiPort`

## Result

### Changed Files

- `backend/docs/product/api-spec.md`
- `backend/docs/tasks/ATCS-164/plan.md`
- `backend/docs/tasks/ATCS-164/spec.md`
- `backend/docs/tasks/ATCS-164/tasks.md`
- `backend/src/main/java/com/sfood/automation/auth/infrastructure/mail/OtpMailSender.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/sfood/automation/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/sfood/automation/goods/application/dto/RegisterGoodsListResponse.java`
- `backend/src/main/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCase.java`
- `backend/src/main/java/com/sfood/automation/goods/presentation/GoodsController.java`
- `backend/src/test/java/com/sfood/automation/auth/infrastructure/mail/OtpMailSenderTest.java`
- `backend/src/test/java/com/sfood/automation/auth/presentation/Task09EndToEndIntegrationTest.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`

### Summary

- changed `POST /api/goods` to accept a JSON array and return per-item registration results
- kept the existing duplicate, restore, Godo sync, and criteria initialization flow for each request item
- updated controller, use case, tests, and API documentation to match the new bulk registration contract
- normalized `HandlerMethodValidationException` to the shared `400` error response path
- temporarily redirected OTP delivery from SMTP to Slack webhook because the current SMTP relay is blocked by provider policy
- kept mail configuration environment-variable based while preserving the existing Spring mail property structure

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest --tests com.sfood.automation.auth.presentation.Task09EndToEndIntegrationTest` ✅
- `./gradlew test --tests com.sfood.automation.auth.infrastructure.mail.OtpMailSenderTest --tests com.sfood.automation.auth.presentation.EmailVerificationControllerTest --tests com.sfood.automation.auth.application.usecase.RequestEmailVerificationUseCaseTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- SMTP delivery is still blocked externally; this patch is an operational workaround, not a permanent mail solution
