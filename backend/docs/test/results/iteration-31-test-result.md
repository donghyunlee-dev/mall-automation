# Iteration 31 Test Result

## Commands

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockClientTest --tests com.sfood.automation.wms.infrastructure.scraping.WmsStockAggregatorTest`
- `./gradlew test --tests com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicyTest`
- `./gradlew test --tests com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicyTest --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest`
- `./gradlew test`

## Results

- targeted WMS stock tests: PASS
- initial missing-refined-stock policy test: FAIL before implementation
- targeted sold-out policy and sync use case tests: PASS
- full test suite: PASS
- final full test result: `BUILD SUCCESSFUL in 36s`

## Notes

- the first targeted compile failed because `WmsStockItem` did not yet expose `lalocQty`
- after remapping `WmsStockItem` to `LALOC_QTY` and updating aggregation to sum `lalocQty`, targeted and full tests passed
- live WMS inspection for ERP code `121201035` returned only excluded `SHP` rows with `LALOC_QTY=0`, so the refined stock map did not contain the item
- the sold-out policy now treats missing refined stock for on-sale goods as zero available stock
