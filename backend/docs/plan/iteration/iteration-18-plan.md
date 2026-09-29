# Iteration 18 Plan

## Scope

- change the common status config update API to use `scope` as a path identifier
- remove `scope` from the update request body so only editable fields remain
- verify the updated endpoint contract with focused tests and full backend validation

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.presentation.CommonStatusConfigControllerTest`
- `./gradlew test`
- `./gradlew build`
