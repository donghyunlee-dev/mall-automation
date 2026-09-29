# ATCS-180 Tasks

## Development Tasks

- add a focused registration test for restoring hidden goods with Godo sold-out state refresh
- patch the registration use case to reapply Godo sync data in the restore path
- add a focused parser test using the live Godo goods search XML structure
- patch the Godo goods search parser so live responses populate `soldOutYn`
- record scoped validation and delivery results in the iteration docs

## Test Cases To Implement

- hidden goods re-registration restores the goods and persists the latest Godo `soldOutYn`
- live `Goods_Search.php` XML with nested `goods_data.soldOutFl=y` is parsed as `soldOutYn=Y`

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCase.java`
- `src/main/java/com/sfood/automation/goods/infrastructure/external/GoodsSearchApiCaller.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/external/GoodsSearchApiCallerTest.java`
- `docs/tasks/ATCS-180/plan.md`
- `docs/tasks/ATCS-180/spec.md`
- `docs/tasks/ATCS-180/tasks.md`
- `docs/plan/iteration/iteration-21-plan.md`
- `docs/test/results/iteration-21-test-result.md`
- `docs/reports/iteration-21-result.md`
- `docs/plan/iteration/iteration-22-plan.md`
- `docs/test/results/iteration-22-test-result.md`
- `docs/reports/iteration-22-result.md`

## New Classes Or Components

- none

## Integration Points

- `GoodsController -> RegisterGoodsUseCase -> GodoApiPort -> GoodsRepository`

## Result

### Changed Files

- `docs/tasks/ATCS-180/plan.md`
- `docs/tasks/ATCS-180/spec.md`
- `docs/tasks/ATCS-180/tasks.md`
- `docs/plan/iteration/iteration-21-plan.md`
- `docs/test/results/iteration-21-test-result.md`
- `docs/reports/iteration-21-result.md`
- `src/main/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCase.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `docs/plan/iteration/iteration-22-plan.md`
- `docs/test/results/iteration-22-test-result.md`
- `docs/reports/iteration-22-result.md`
- `src/main/java/com/sfood/automation/goods/infrastructure/external/GoodsSearchApiCaller.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/external/GoodsSearchApiCallerTest.java`

### Summary

- restored hidden goods now refresh Godo sold-out state during registration before the entity is saved
- added focused registration coverage proving the restored goods record persists the Godo `soldOutYn`
- fixed the live Godo goods search XML parsing so nested `soldOutFl` values populate `soldOutYn` during registration
- recorded the scoped Jira delivery and validation results in the task and iteration docs

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest`
- passed: `./gradlew test`
- passed: `./gradlew build`
- passed: `./gradlew test --tests com.sfood.automation.goods.infrastructure.external.GoodsSearchApiCallerTest --tests com.sfood.automation.goods.infrastructure.external.GodoApiClientTest --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest`

### Review Notes

- no prior local task artifacts or matching GitHub PRs were found for `ATCS-180`
- the change set stays inside the goods registration use case and its focused tests
- live database inspection showed the production `GOODS_SEARCH` response stored `<soldOutFl>y</soldOutFl>` under `data -> return -> goods_data`, which the previous parser did not read
