# Iteration 18 Result

## Delivered Scope

- changed the common status config update endpoint from body-scoped lookup to REST-style path-scoped lookup
- removed `scope` from the update request payload while preserving the existing success response shape
- added focused automated coverage and updated backend API documentation for the new contract

## Key Decisions

- bound `scope` as a `GoodsScope` path variable in the controller to keep identifier handling at the API boundary
- kept the repository and domain model unchanged because `common_status_config.scope` already serves as the lookup key
- limited documentation updates to the task files, iteration files, and public API spec to avoid unrelated scope expansion

## Validation

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.presentation.CommonStatusConfigControllerTest`
- `./gradlew test`
- `./gradlew build`

## Open Gaps

- no git publishing steps were performed because repository rules require an explicit user request before branch or PR operations
