# ATCS-165 Tasks

## Development Tasks

- add duplicate ERP-code validation to the goods update use case
- expose business-exception messages through the shared error response
- keep the existing successful update behavior unchanged
- update API and task documentation for the refined error behavior

## Test Cases To Implement

- update goods successfully when keeping the same ERP code
- reject updating goods to another goods row's ERP code
- return the business-exception message in the error response payload

## Modules To Modify

- `src/main/java/com/sfood/automation/common/exception`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/common/exception`
- `src/test/java/com/sfood/automation/goods/application/usecase`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/product/api-spec.md`

## New Classes Or Components

- none expected

## Integration Points

- `GoodsRepository`
- `CommonStatusConfigRepository`

## Result

### Changed Files

- `backend/docs/product/api-spec.md`
- `backend/docs/tasks/ATCS-165/plan.md`
- `backend/docs/tasks/ATCS-165/spec.md`
- `backend/docs/tasks/ATCS-165/tasks.md`
- `backend/src/main/java/com/sfood/automation/common/exception/DuplicateGoodsException.java`
- `backend/src/main/java/com/sfood/automation/common/exception/ErrorResponse.java`
- `backend/src/main/java/com/sfood/automation/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCase.java`
- `backend/src/test/java/com/sfood/automation/common/exception/GlobalExceptionHandlerTest.java`
- `backend/src/test/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCaseTest.java`
- `backend/src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`

### Summary

- prevented duplicate ERP-code updates from reaching the database as unhandled `500` errors
- returned the business-exception message in the shared error payload so update failures expose a readable cause
- added focused use case, controller, and exception-handler coverage for the update path

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateGoodsUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest --tests com.sfood.automation.common.exception.GlobalExceptionHandlerTest` ✅
- `./gradlew test` ✅
- `./gradlew build` ✅

### Review Notes

- current fix targets the known duplicate ERP-code update failure path without expanding unrelated exception behavior
