# ATCS-211 Tasks

## Development Tasks

- add focused parser coverage for `code=000` responses with empty `return`
- add focused registration tests for unmatched Godo goods search results
- patch the registration flow so it only applies Godo sync when the returned `goodsNo` matches the requested goods code
- record scoped validation and delivery results in the task and iteration docs

## Test Cases To Implement

- `Goods_Search` empty `return` parses without throwing and returns blank goods identity
- new goods registration keeps `godo_sync_yn='N'` when the Godo response has no matching `goodsNo`
- hidden goods restore resets `godo_sync_yn` to `N` when the Godo response has no matching `goodsNo`

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCase.java`
- `src/main/java/com/sfood/automation/goods/domain/model/GodoGoodsInfo.java`
- `src/main/java/com/sfood/automation/goods/domain/model/Goods.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/external/GoodsSearchApiCallerTest.java`
- `docs/tasks/ATCS-211/plan.md`
- `docs/tasks/ATCS-211/spec.md`
- `docs/tasks/ATCS-211/tasks.md`
- `docs/plan/iteration/iteration-25-plan.md`
- `docs/test/results/iteration-25-test-result.md`
- `docs/reports/iteration-25-result.md`

## New Classes Or Components

- none

## Integration Points

- `GoodsController -> RegisterGoodsUseCase -> GodoApiPort -> GoodsRepository`

## Result

### Changed Files

- `docs/tasks/ATCS-211/plan.md`
- `docs/tasks/ATCS-211/spec.md`
- `docs/tasks/ATCS-211/tasks.md`
- `docs/plan/iteration/iteration-25-plan.md`
- `docs/test/results/iteration-25-test-result.md`
- `docs/reports/iteration-25-result.md`
- `src/main/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCase.java`
- `src/main/java/com/sfood/automation/goods/domain/model/GodoGoodsInfo.java`
- `src/main/java/com/sfood/automation/goods/domain/model/Goods.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/external/GoodsSearchApiCallerTest.java`

### Summary

- fixed goods registration so Godo sync is only applied when the returned `goodsNo` matches the searched goods code
- kept empty or mismatched `Goods_Search` responses as unsynced while still preserving transport-level API success logging
- added focused coverage for empty `return` parsing and for new or restored goods registration with unmatched Godo search results

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest --tests com.sfood.automation.goods.infrastructure.external.GoodsSearchApiCallerTest`
- failed: `./gradlew build`
  - existing failures in `SyncGodoStatusUseCaseTest`

### Review Notes

- no prior local task artifacts or matching GitHub PRs were found for `ATCS-211`
- the fix stays inside goods registration and the Godo search interpretation path
- existing batch status sync test failures remain outside this task scope
