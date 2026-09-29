# Iteration 29 Test Result

## Commands

- `./gradlew test --tests com.sfood.automation.goods.presentation.GoodsListControllerTest`
- `./gradlew test`

## Results

- targeted goods list controller test: PASS
- full test suite: PASS
- final full test result: `BUILD SUCCESSFUL in 38s`

## Notes

- the first targeted run failed at compile time because `GoodsListItemResponse` did not yet include `godoSyncBlockedYn`
- after adding the field to the list DTO and mapper, the targeted controller test and full suite both passed
