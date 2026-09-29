# WMS Missing Log Defaults Result

## Delivered Scope

- Updated product sync status log creation so missing WMS data writes `0` for both `valid_percent` and `remain_valid_days`.
- Kept the change limited to the missing-WMS batch path.

## Changed Files

- [SyncGodoStatusUseCase.java](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java)
- [SyncGodoStatusUseCaseTest.java](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java)
- [spec.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/tasks/wms-missing-log-defaults/spec.md)
- [plan.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/tasks/wms-missing-log-defaults/plan.md)
- [tasks.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/tasks/wms-missing-log-defaults/tasks.md)
- [test-result.md](/home/donghyunlee/projects/SFOOD-MALL-AUTOMATION/backend/docs/tasks/wms-missing-log-defaults/test-result.md)

## Key Decisions

- Use `0` as the neutral non-null sentinel when WMS stock data is completely missing.
- Keep existing behavior unchanged when an `AggregatedStock` is present, even if its nullable fields are empty.

## Open Gaps

- None for this fix.

## Next Steps

- None.
