# ATCS-224 Tasks

## Development Tasks

- add force-sync request and response DTOs
- add a dedicated force-sync use case
- add an authenticated controller endpoint
- add item-level eligibility validation and result mapping
- persist local goods update and goods status log only on successful Godo API calls

## Test Cases

- should force-sync an eligible goods item and update local sold-out state
- should skip blocked goods without calling Godo API
- should skip non-normal goods without calling Godo API
- should return not-found result for unknown Godo goods codes
- should not update goods or logs when the Godo API fails
- should expose the new endpoint through controller tests

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/presentation`
- `src/main/java/com/sfood/automation/goods/application/dto`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `docs/product/api-spec.md`

## New Classes Or Components

- `ForceSyncGodoStatusRequest`
- `ForceSyncGodoStatusResponse`
- `ForceSyncGodoStatusItemResponse`
- `ForceSyncGodoStatusUseCase`

## Integration Points

- `GodoApiPort`
- `GoodsRepository`
- `GoodsStatusLogRepository`
- authenticated session user extraction in `GoodsController`

## Result

### Changed Files

- `src/main/java/com/sfood/automation/goods/application/dto/ForceSyncGodoStatusRequest.java`
- `src/main/java/com/sfood/automation/goods/application/dto/ForceSyncGodoStatusItemResponse.java`
- `src/main/java/com/sfood/automation/goods/application/dto/ForceSyncGodoStatusResponse.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCase.java`
- `src/main/java/com/sfood/automation/goods/presentation/GoodsController.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsListControllerTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsStatusLogControllerTest.java`
- `docs/product/api-spec.md`

### Summary

- added a manual Godo force-sync API and a dedicated use case separate from batch status sync

### Test Results

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCaseTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest` PASS
- `./gradlew test` PASS

### Review Notes

- manual force sync validates goods eligibility only and does not reuse WMS stock policy checks
