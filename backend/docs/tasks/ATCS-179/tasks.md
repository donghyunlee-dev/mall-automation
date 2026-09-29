# ATCS-179 Tasks

## Development Tasks

- add failing tests for the goods detail response contract
- extend the detail response DTO and mapper with the missing goods fields
- update the detail endpoint API documentation

## Test Cases To Implement

- goods detail controller response includes the newly exposed fields
- goods detail mapping preserves nullable values for optional fields

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/dto/GoodsResponse.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/GoodsMapper.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/GetGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`
- `docs/product/api-spec.md`

## New Classes Or Components

- none beyond the focused use-case test class

## Integration Points

- `GoodsController -> GetGoodsUseCase -> GoodsMapper -> GoodsResponse`

## Result

### Changed Files

- `docs/tasks/ATCS-179/plan.md`
- `docs/tasks/ATCS-179/spec.md`
- `docs/tasks/ATCS-179/tasks.md`
- `docs/plan/iteration/iteration-20-plan.md`
- `docs/product/api-spec.md`
- `docs/reports/iteration-20-result.md`
- `docs/test/results/iteration-20-test-result.md`
- `src/main/java/com/sfood/automation/goods/application/dto/GoodsResponse.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/GoodsMapper.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/GetGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`

### Summary

- expanded `GET /api/goods/{id}` to expose the missing goods detail fields: `description`, `createdId`, `createdAt`, `updatedId`, and `updatedAt`
- added focused use-case and controller coverage for the widened detail response contract
- updated the product API spec and iteration documentation for this task scope

### Test Results

- passed: `./gradlew test --tests com.sfood.automation.goods.application.usecase.GetGoodsUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest`

### Review Notes

- change set intentionally excludes unrelated list and batch work already in progress in other branches/worktrees
