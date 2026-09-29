# ATCS-175 Tasks

## Development Tasks

- extend the goods update request DTO with per-item criteria fields
- persist the new criteria values in the goods update use case
- cover the extended update contract with focused controller and use case tests
- update product documentation and Jira task docs for the new request payload

## Test Cases To Implement

- update goods successfully with explicit `availableQty` and `safetyRate`
- keep duplicate ERP validation behavior on update
- reject invalid update payloads for the new numeric fields

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/dto`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/product`
- `docs/tasks/ATCS-175`

## New Classes Or Components

- none

## Integration Points

- `GoodsController`
- `UpdateGoodsUseCase`
- `UpdateGoodsRequest`
- `docs/product/api-spec.md`

## Result

### Changed Files

- `backend/docs/product/api-spec.md`
- `backend/docs/product/prd.md`
- `backend/docs/tasks/ATCS-175/plan.md`
- `backend/docs/tasks/ATCS-175/spec.md`
- `backend/docs/tasks/ATCS-175/tasks.md`
- `backend/src/main/java/com/sfood/automation/goods/application/dto/UpdateGoodsRequest.java`
- `backend/src/main/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCase.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`

### Summary

- extended the goods update request with `availableQty` and `safetyRate`
- changed the goods update use case to persist the request criteria values directly instead of recalculating from common scope config
- added controller and use case coverage for successful updates, duplicate ERP rejection, and invalid negative criteria
- updated the product API and PRD documentation for the new update request contract

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateGoodsUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- `./gradlew test` and `./gradlew build` were re-run sequentially because parallel execution can collide on shared Gradle test result files and the shared SQLite test DB
- git branch, commit, push, and PR creation remain out of scope because `backend/AGENTS.md` requires an explicit user request for repository operations
