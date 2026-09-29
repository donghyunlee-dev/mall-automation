# Iteration 30 Test Result

## Commands

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest`
- `./gradlew test`

## Results

- targeted force-sync tests: PASS
- full test suite: PASS
- final full test result: `BUILD SUCCESSFUL in 1m 25s`

## Notes

- the first targeted compile failed because the new force-sync DTOs and use case were not yet implemented
- after adding the new request, response, controller endpoint, and use case, the targeted tests passed
