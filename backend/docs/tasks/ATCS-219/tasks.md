# ATCS-219 Tasks

## Development Tasks

- add task and iteration planning artifacts for the goods-criteria synchronization bug
- add regression coverage for scope-specific goods synchronization after config updates
- update the config change use case to synchronize matching goods criteria
- record validation and delivery notes for the bug fix

## Test Cases To Implement

- updating `GENERAL` config synchronizes only `GENERAL` goods criteria
- updating `EXCLUSIVE` config synchronizes only `EXCLUSIVE` goods criteria
- repository scope lookup includes hidden goods so restored items do not keep stale criteria

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCase.java`
- `src/main/java/com/sfood/automation/goods/domain/port/GoodsRepository.java`
- `src/main/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImpl.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImplTest.java`
- `docs/tasks/ATCS-219/plan.md`
- `docs/tasks/ATCS-219/spec.md`
- `docs/tasks/ATCS-219/tasks.md`
- `docs/plan/iteration/iteration-28-plan.md`
- `docs/test/results/iteration-28-test-result.md`
- `docs/reports/iteration-28-result.md`

## New Classes Or Components

- none

## Integration Points

- `CommonStatusConfigController -> UpdateCommonStatusConfigUseCase -> GoodsRepository`

## Result

### Changed Files

- `docs/tasks/ATCS-219/plan.md`
- `docs/tasks/ATCS-219/spec.md`
- `docs/tasks/ATCS-219/tasks.md`
- `docs/plan/iteration/iteration-28-plan.md`
- `docs/test/results/iteration-28-test-result.md`
- `docs/reports/iteration-28-result.md`
- `src/main/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCase.java`
- `src/main/java/com/sfood/automation/goods/domain/port/GoodsRepository.java`
- `src/main/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImpl.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/GetGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/GetGoodsListUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/DeleteGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImplTest.java`

### Summary

- synchronized `availableQty` and `safetyRate` for existing goods when `CommonStatusConfig` is updated
- kept synchronization isolated by scope using `onlyMallYn` and included hidden goods so restored items do not keep stale criteria
- added focused regression coverage for the use case and repository scope lookup

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.infrastructure.persistence.GoodsRepositoryImplTest`
- passed: `./gradlew test`
- passed: `./gradlew build`

### Review Notes

- `onSaleQty` remains stored in `common_status_config` only because current transition policy still derives on-sale recovery from `goods.availableQty + 1`
- scope synchronization uses the `GoodsRepository` port to avoid coupling the config use case to persistence details
