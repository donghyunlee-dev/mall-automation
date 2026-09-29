# Iteration 25 Plan

## Scope

- fix Godo goods registration sync so `godo_sync_yn` depends on actual matching goods data, not only `code=000`
- cover empty and mismatched Godo search results with focused tests
- keep the change scoped to registration and Godo search interpretation

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest --tests com.sfood.automation.goods.infrastructure.external.GoodsSearchApiCallerTest`
