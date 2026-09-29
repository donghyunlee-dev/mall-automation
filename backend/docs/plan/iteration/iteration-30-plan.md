# Iteration 30 Plan

## Scope

- add a manual Godo status force-sync API for selected goods
- validate only manual-sync eligibility rules and bypass batch stock policy checks
- persist local sold-out updates and status logs on successful Godo sync results

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest`
- `./gradlew test`
