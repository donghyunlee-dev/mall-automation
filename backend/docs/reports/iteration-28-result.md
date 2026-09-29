# Iteration 28 Result

## Delivered Scope

- fixed config update flow so existing goods criteria are synchronized when `CommonStatusConfig` changes
- kept `GENERAL` and `EXCLUSIVE` updates isolated to their matching goods scope
- added regression coverage for use case synchronization and repository scope lookup including hidden goods

## Key Decisions

- synchronized goods inside `UpdateCommonStatusConfigUseCase`, which already owns the config change transaction
- added a scope lookup method to `GoodsRepository` so the use case stays interface-driven and can update hidden goods as well
- left `GoodsSoldOutPolicy` unchanged because the bug was stale goods criteria, not transition threshold logic

## Validation

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.UpdateCommonStatusConfigUseCaseTest --tests com.sfood.automation.goods.infrastructure.persistence.GoodsRepositoryImplTest`
- `./gradlew test`
- `./gradlew build`

## Next Steps

- if `onSaleQty` should become an operator-configurable threshold, connect it explicitly to the on-sale transition policy in a separate task
