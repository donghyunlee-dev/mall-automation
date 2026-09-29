# Iteration 28 Plan

## Scope

- synchronize existing goods criteria when `CommonStatusConfig` is updated
- keep scope isolation between `GENERAL` and `EXCLUSIVE`
- cover the regression with focused use case and repository tests

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.infrastructure.persistence.GoodsRepositoryImplTest`
- `./gradlew test`
