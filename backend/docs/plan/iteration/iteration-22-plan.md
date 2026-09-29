# Iteration 22 Plan

## Scope

- fix the Godo goods search parser to read the live XML response structure used in production
- add focused parser coverage for nested `goods_data.soldOutFl`
- revalidate the registration path tests affected by the parser output

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.infrastructure.external.GoodsSearchApiCallerTest --tests com.sfood.automation.goods.infrastructure.external.GodoApiClientTest --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest`



