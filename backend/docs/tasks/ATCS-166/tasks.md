# ATCS-166 Tasks

## Development Tasks

- add a bulk-read method to the common status config repository port and persistence adapter
- change the read use case and controller to return the full config list without a `scope` parameter
- update API documentation to describe the array response contract

## Test Cases To Implement

- return both `GENERAL` and `EXCLUSIVE` configs from the read use case
- return a JSON array from `GET /api/config/status`
- keep the existing config update endpoint behavior intact

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/domain/port`
- `src/main/java/com/sfood/automation/goods/infrastructure/persistence`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/main/java/com/sfood/automation/goods/presentation`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/product/api-spec.md`

## New Classes Or Components

- `GetCommonStatusConfigUseCaseTest`

## Integration Points

- `CommonStatusConfigJpaRepository`
- `CommonStatusConfigRepository`
- `CommonStatusConfigController`

## Result

### Changed Files

- `backend/docs/product/api-spec.md`
- `backend/docs/tasks/ATCS-166/plan.md`
- `backend/docs/tasks/ATCS-166/spec.md`
- `backend/docs/tasks/ATCS-166/tasks.md`
- `backend/src/main/java/com/sfood/automation/goods/application/usecase/GetCommonStatusConfigUseCase.java`
- `backend/src/main/java/com/sfood/automation/goods/domain/port/CommonStatusConfigRepository.java`
- `backend/src/main/java/com/sfood/automation/goods/infrastructure/persistence/CommonStatusConfigJpaRepository.java`
- `backend/src/main/java/com/sfood/automation/goods/infrastructure/persistence/CommonStatusConfigRepositoryImpl.java`
- `backend/src/main/java/com/sfood/automation/goods/presentation/CommonStatusConfigController.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/GetCommonStatusConfigUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/presentation/CommonStatusConfigControllerTest.java`

### Summary

- changed `GET /api/config/status` to always return the full common status config list
- removed the read endpoint's `scope` query-parameter dependency from the controller and use case
- added focused controller and use case coverage for the new array response contract

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.GetCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.presentation.CommonStatusConfigControllerTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- repository list retrieval is ordered by persisted row id so the default seeded response remains `GENERAL`, then `EXCLUSIVE`
- branch checkout, commit, push, and PR creation were not performed because `backend/AGENTS.md` requires an explicit user request for git operations
