# Iteration 19 Plan

## Scope

- add recent modification metadata to the goods list API response items
- fall back to creation metadata when goods update metadata is absent
- update focused tests and backend API documentation for the list endpoint contract

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.GetGoodsListUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsListControllerTest`
- `./gradlew test`
- `./gradlew build`
