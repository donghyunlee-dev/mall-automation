# Iteration 31 Result

## Outcome Summary

- changed WMS stock quantity mapping for batch decisions from `INVN_QTY` to `LALOC_QTY`
- updated the WMS aggregator to sum the mapped `LALOC_QTY` value
- fixed the sold-out policy so on-sale goods with no refined eligible WMS stock are evaluated as zero available stock
- updated product documentation to describe `LALOC_QTY` as the batch quantity basis

## Commands Executed

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockClientTest --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregatorTest`
- `./gradlew test --tests com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicyTest`
- `./gradlew test --tests com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicyTest --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest`
- `./gradlew test`

## Test Results

- targeted WMS stock coverage: PASS
- targeted sold-out policy and sync use case coverage: PASS
- repository-wide test suite: PASS

## Notable Findings

- ERP code `121201035` currently appears in WMS only as excluded `SHP` rows with `LALOC_QTY=0`, so it is removed by stock refinement before aggregation
- before this fix, missing refined stock returned `NO_CHANGE`; after this fix it triggers sold-out transition for goods currently on sale
- `INVN_QTY` remains present in the WMS test fixture only to prove that parsing now uses `LALOC_QTY` instead
