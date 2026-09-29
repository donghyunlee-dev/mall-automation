# Iteration 31 Plan

## Scope

- change WMS batch stock quantity source from `INVN_QTY` to `LALOC_QTY`
- keep existing batch eligibility rules, zone filtering, lot checks, and sold-out policy thresholds unchanged
- treat missing refined WMS stock for an on-sale registered goods item as zero available stock for sold-out evaluation
- update WMS mapping tests and related product documentation

## Validation Plan

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockClientTest --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregatorTest`
- `./gradlew test --tests com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicyTest --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest`
- `./gradlew test`
