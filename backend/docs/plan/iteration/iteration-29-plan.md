# Iteration 29 Plan

## Scope

- add a goods-level automatic Godo sync block setting
- expose a dedicated API for toggling the block setting
- skip blocked goods in the product sync batch before any Godo API call
- expose the block flag in the goods list response used by the frontend

## Validation Plan

- `./gradlew test --tests com.sfood.automation.goods.presentation.GoodsListControllerTest`
- `./gradlew test`
