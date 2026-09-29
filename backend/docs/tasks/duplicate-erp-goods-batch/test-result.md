# duplicate-erp-goods-batch Test Result

## Scope

- Validate duplicate ERP goods support in persistence validation and batch stock evaluation.
- Confirm the existing WMS fetch/refine path still works against the live WMS endpoint without calling live Godo updates.

## Environment

- Profile:
  - `local`-equivalent test configuration
- Database:
  - shared SQLite test DB at `data/sfood-mall-automation_test.db`
- External dependencies:
  - live WMS only for the guarded integration test

## Commands

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.RegisterGoodsUseCaseTest --tests com.sfood.automation.goods.application.usecase.UpdateGoodsUseCaseTest --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest --tests com.sfood.automation.goods.infrastructure.persistence.GoodsRepositoryImplTest --tests com.sfood.automation.goods.presentation.GoodsControllerTest`
- `./gradlew test`
- `LIVE_WMS=true ./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.LiveWmsRefinementIntegrationTest`

## Results

- PASS:
  - targeted duplicate-ERP regression suite passed
  - full Gradle test suite passed
  - guarded live WMS integration test passed
- FAIL:
  - initial targeted run failed because the shared SQLite test DB still had the legacy unique ERP schema
  - initial full suite run failed because the force-sync contract documentation still described the older `NORMAL` and `ERP sync Y` gates

## Failure Notes

- The shared SQLite test DB reused an older schema with a unique ERP constraint, so duplicate ERP inserts failed until the test DB compatibility helper reset legacy schemas.
- The manual force-sync documentation had drifted from the relaxed `develop` contract; the current branch keeps only `godoSyncYn == Y` and `godoSyncBlockedYn == N` as gates.

## Retest Results

- After resetting legacy SQLite test schemas, the targeted suite passed.
- After removing the stale `NORMAL` / ERP-sync requirements from the force-sync contract docs, the branch state and the documented behavior stayed aligned.

## Acceptance Criteria Coverage

- AC1:
  - covered by `RegisterGoodsUseCaseTest`, `UpdateGoodsUseCaseTest`, and `GoodsRepositoryImplTest`
- AC2:
  - covered by `ProductSyncBatchJobTest` and `SyncGodoStatusUseCaseTest`
- AC3:
  - covered by `StockDataRefinerTest` duplicate-ERP goods scenarios
- AC4:
  - covered by the unchanged single-goods regression tests plus the full `./gradlew test` run
