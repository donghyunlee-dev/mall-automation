# ATCS-198 Tasks

## Development Tasks

- Enforce required validation for `availableQty` and `safetyRate` in `UpdateGoodsRequest`.
- Keep the existing update use case and persistence flow unchanged unless validation reveals a gap.
- Update API documentation examples for the goods update endpoint.

## Test Cases To Implement

- Update succeeds when both criteria fields are present.
- Update fails when `availableQty` is missing.
- Update fails when `safetyRate` is null.
- Update fails when criteria values are negative.

## Modules To Modify

- `src/main/java/com/sfood/automation/goods/application/dto`
- `src/test/java/com/sfood/automation/goods/presentation`
- `docs/product`

## New Classes Or Components

- None expected.

## Integration Points

- Spring MVC request binding and Bean Validation for `PUT /api/goods/{id}`.
- Existing `UpdateGoodsUseCase` and `GoodsRepository` persistence path.

## Result

- Changed files:
  - `src/main/java/com/sfood/automation/goods/application/dto/UpdateGoodsRequest.java`
  - `src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`
  - `docs/product/api-spec.md`
  - `docs/tasks/ATCS-198/plan.md`
  - `docs/tasks/ATCS-198/spec.md`
  - `docs/tasks/ATCS-198/tasks.md`
- Summary:
  - enforced `availableQty` and `safetyRate` as required update request fields
  - added controller coverage for missing and null criteria validation failures
  - aligned goods update API documentation with the current persisted response values
- Test results:
  - `./gradlew test --tests com.sfood.automation.goods.presentation.GoodsControllerTest --tests com.sfood.automation.goods.application.usecase.UpdateGoodsUseCaseTest` passed
- Review notes:
  - existing goods update use case already persisted the criteria values correctly
  - unrelated local iteration-22 work remains in the working tree and was left untouched
