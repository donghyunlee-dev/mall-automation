# ATCS-177 Tasks

## Development Tasks

- add a list-specific goods response item that carries recent modification metadata
- map the latest modification fields with created-data fallback in the goods list use case
- update the list endpoint documentation and Jira iteration artifacts
- add focused tests for fallback mapping and list-response JSON serialization

## Test Cases To Implement

- return `recentUpdatedAt` and `recentUpdatedBy` from update metadata when present
- fall back to created metadata when update metadata is absent
- serialize the new list response fields from `GET /api/goods`

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/dto`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/product/api-spec.md`
- `docs/tasks/ATCS-177`
- `docs/plan/iteration/iteration-19-plan.md`
- `docs/test/results/iteration-19-test-result.md`
- `docs/reports/iteration-19-result.md`

## New Classes Or Components

- `GoodsListItemResponse`

## Integration Points

- `GetGoodsListUseCase`
- `GoodsController`
- frontend consumers of `GET /api/goods`

## Result

### Changed Files

- `src/main/java/com/sfood/automation/goods/application/dto/GoodsListItemResponse.java`
- `src/main/java/com/sfood/automation/goods/application/dto/GoodsListResponse.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/GoodsMapper.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/GetGoodsListUseCase.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/GetGoodsListUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsListControllerTest.java`
- `docs/product/api-spec.md`
- `docs/tasks/ATCS-177/plan.md`
- `docs/tasks/ATCS-177/spec.md`
- `docs/tasks/ATCS-177/tasks.md`
- `docs/plan/iteration/iteration-19-plan.md`
- `docs/test/results/iteration-19-test-result.md`
- `docs/reports/iteration-19-result.md`

### Summary

- added `recentUpdatedAt` and `recentUpdatedBy` only to goods list items
- applied fallback mapping from update metadata to creation metadata when goods were never edited
- updated focused tests and the goods list API documentation for the new response contract

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.GetGoodsListUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsListControllerTest` passed
- `./gradlew test` failed because `Task09EndToEndIntegrationTest` still fails at line 154 and line 267
- `./gradlew build` failed because the same `Task09EndToEndIntegrationTest` failures cause `:test` to fail

### Review Notes

- no git branch, commit, push, or PR steps were performed because repository rules require an explicit user request
- the full-suite failures occur before the goods list endpoint under this task is exercised, so they do not point to the ATCS-177 response change
