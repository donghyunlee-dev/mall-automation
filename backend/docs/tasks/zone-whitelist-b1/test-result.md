# zone-whitelist-b1 Test Result

## Scope

- `B1` zone 허용과 비허용 영문 zone 제외가 정제/정책/배치 경로에서 일관되게 동작하는지 검증

## Environment

- Profile:
  local test
- Database:
  SQLite test fixtures
- External dependencies:
  없음

## Commands

- `./gradlew test --tests com.sfood.automation.wms.infrastructure.scraping.StockDataRefinerTest --tests com.sfood.automation.goods.domain.policy.GoodsSoldOutPolicyTest --tests com.sfood.automation.batch.infrastructure.job.ProductSyncBatchJobTest --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest`
- `./gradlew test`

## Results

- PASS:
  Focused regression tests passed for refiner, policy, batch job, and sync use case.
  Full `./gradlew test` passed.
- FAIL:
  None

## Failure Notes

- None

## Retest Results

- Full suite remained green after the zone eligibility refactor.

## Acceptance Criteria Coverage

- AC1:
  `StockDataRefinerTest.should_include_b1_zone_rows_as_allowed_stock` verifies `B1` rows are included.
- AC2:
  Existing numeric zone tests continue to pass.
- AC3:
  `SHP` rows are excluded in refiner and batch job tests.
- AC4:
  `GoodsSoldOutPolicyTest` now evaluates zone eligibility via `allowedZone`.
- AC5:
  Product docs and task docs updated to reflect `numeric + whitelist(B1)` behavior.
