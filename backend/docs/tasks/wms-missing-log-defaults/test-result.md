# WMS Missing Log Defaults Test Result

## Validation Scope

- Verify that the batch status log writes `0` for `valid_percent` and `remain_valid_days` when WMS data is missing.
- Confirm that existing batch use-case paths still pass.

## Commands Executed

- `./gradlew test --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest`
- `./gradlew test --tests com.sfood.automation.goods.application.usecase.SyncGodoStatusUseCaseTest --stacktrace`

## Results

- PASS: `SyncGodoStatusUseCaseTest` passed after the fix.
- FAIL: Initial run failed because the ternary expression unboxed a null `remainValidDays()` value.
- PASS: After changing the missing-WMS defaults to `Integer.valueOf(0)`, the focused test suite passed.

## Coverage Notes

- The regression coverage now checks both `validPercent` and `remainValidDays` on the missing-WMS transition path.

## Retest Notes

- Retest confirmed that the fix does not break the existing transition scenarios covered by the test class.
