# ATCS-236 Test Result

## Scope

- Validate that manual force-sync no longer fails when persisting `goods_status_log`.

## Environment

- Profile: local test execution
- Database: test runtime
- External dependencies: mocked / stubbed

## Commands

- `GRADLE_USER_HOME=/tmp/gradle ./gradlew test --tests ForceSyncGodoStatusUseCaseTest --tests GoodsControllerTest`

## Results

- PASS: Manual force-sync use-case and controller tests passed after the status log fix.
- PASS: The status log now stores a non-null `valid_percent`.

## Failure Notes

- The original failure was a runtime constraint violation on `goods_status_log.valid_percent`.

## Retest Results

- Retest passed after changing the manual force-sync log builder to write `0` for `valid_percent`.

## Acceptance Criteria Coverage

- AC1: Covered
- AC2: Covered
- AC3: Covered
