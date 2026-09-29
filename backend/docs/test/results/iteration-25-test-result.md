# Iteration 25 Test Result

## Commands

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest --tests com.sfood.automation.goods.infrastructure.external.GoodsSearchApiCallerTest`
- `./gradlew build`

## Results

- focused ATCS-211 tests: PASS
- full build: FAIL

## Failures

- `./gradlew build`
  - `SyncGodoStatusUseCaseTest.should_recover_on_sale_using_goods_available_qty_after_safety_filtered_aggregation()`
  - `SyncGodoStatusUseCaseTest.should_return_sync_result_counts()`
  - `SyncGodoStatusUseCaseTest.should_process_out_of_stock_before_on_sale()`

## Notes

- the new ATCS-211 parser and registration tests passed after the fix
- the remaining build failures are pre-existing and outside the ATCS-211 write scope
