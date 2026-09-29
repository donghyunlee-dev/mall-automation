# ATCS-167 Tasks

## Development Tasks

- move the common status config update identifier from the request body to the controller path
- remove `scope` from the update request DTO and adapt the update use case signature
- update the API specification to document the new endpoint contract

## Test Cases To Implement

- accept `PUT /api/config/status/{scope}` with a body that excludes `scope`
- update the selected config using the path scope and authenticated session email
- keep validation for negative threshold values intact

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/dto`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/main/java/com/sfood/automation/goods/presentation`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/product/api-spec.md`

## New Classes Or Components

- `UpdateCommonStatusConfigUseCaseTest`

## Integration Points

- `CommonStatusConfigController`
- `UpdateCommonStatusConfigUseCase`
- `CommonStatusConfigRepository`

## Result

### Changed Files

- `backend/docs/plan/iteration/iteration-18-plan.md`
- `backend/docs/product/api-spec.md`
- `backend/docs/tasks/ATCS-167/plan.md`
- `backend/docs/tasks/ATCS-167/spec.md`
- `backend/docs/tasks/ATCS-167/tasks.md`
- `backend/src/main/java/com/sfood/automation/goods/application/dto/UpdateCommonStatusConfigRequest.java`
- `backend/src/main/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCase.java`
- `backend/src/main/java/com/sfood/automation/goods/presentation/CommonStatusConfigController.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/presentation/CommonStatusConfigControllerTest.java`

### Summary

- changed the update endpoint to `PUT /api/config/status/{scope}` so the resource identifier is carried in the URL
- removed `scope` from the update request DTO and routed the selected `GoodsScope` through the controller path variable
- added focused use case and controller coverage for the new request contract and documented it in the API spec

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.presentation.CommonStatusConfigControllerTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- git branch, commit, push, and PR creation were not performed because `backend/AGENTS.md` requires an explicit user request for repository operations
- full test output included existing `@MockBean` deprecation warnings in unrelated controller tests, but the build remained green
