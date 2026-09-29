# ATCS-225 Tasks

## Development Tasks

- [x] Add force-sync request and response types.
- [x] Add `forceSyncGodoStatus` API client function.
- [x] Add sale status force-sync eligibility logic to the product table.
- [x] Make eligible sale status chips clickable.
- [x] Add a confirmation modal for force-sync.
- [x] Wire modal state, confirm handling, reload, and toast behavior in `DashboardPage`.
- [x] Update tests for enabled and disabled chip behavior.
- [x] Add API client tests for the force-sync request shape if feasible.
- [x] Fix backend force-sync status log persistence for the runtime NOT NULL `valid_percent` constraint.

## Test Cases

- [x] Eligible on-sale item opens the force-sync modal.
- [x] Eligible sold-out item opens the force-sync modal.
- [x] Unverified, blocked, or unavailable items do not open the modal.
- [x] Confirming an on-sale item sends `targetSoldOutYn: 'Y'`.
- [x] Confirming a sold-out item sends `targetSoldOutYn: 'N'`.
- [x] Non-success item result displays an error.
- [x] Backend force-sync success stores a non-null status log `validPercent`.

## Modules To Modify

- `src/api/goods.ts`
- `src/api/goods.test.ts`
- `src/types/goods.ts`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`
- `src/components/modals/ForceGodoStatusConfirmModal.tsx`
- `src/pages/DashboardPage.tsx`
- `../backend/src/main/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCase.java`
- `../backend/src/test/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCaseTest.java`
- `src/components/dashboard/ProductTable.test.tsx`

## New Components

- `ForceGodoStatusConfirmModal`

## Integration Points

- `POST /api/goods/godo-status/force-sync`
- Goods reload flow in `DashboardPage`
- Existing toast system

## Result

### Changed Files

- `docs/tasks/ATCS-225/plan.md`
- `docs/tasks/ATCS-225/spec.md`
- `docs/tasks/ATCS-225/tasks.md`
- `src/api/goods.ts`
- `src/types/goods.ts`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`
- `src/components/dashboard/ProductTable.test.tsx`
- `src/components/modals/ForceGodoStatusConfirmModal.tsx`
- `src/pages/DashboardPage.tsx`

### Summary

- Added the list-based force-sync API client and DTOs.
- Made eligible sale status chips clickable for manual Godo status force-sync.
- Added a confirmation modal that sends the opposite sale state for a single goods code.
- Reloads goods and refreshes status logs only after a successful item-level response.
- Fixed a backend runtime 500 where manual force-sync tried to persist `null` into the non-null `goods_status_log.valid_percent` column.

### Test Results

- `npm test -- --run src/api/goods.test.ts src/components/dashboard/ProductTable.test.tsx src/hooks/useGoods.test.tsx src/components/modals/ProductEditModal.test.tsx src/utils/goodsSaleStatus.test.ts`
- `npm run build`
- `npm run lint`
- `./gradlew test --tests com.sfood.automation.goods.application.usecase.ForceSyncGodoStatusUseCaseTest`
- `npm test -- --run src/api/goods.test.ts src/components/dashboard/ProductTable.test.tsx`

### Review Notes

- API client remains list-based so a future bulk force-sync UI can reuse the same integration.
- HTTP and item-level failures keep the modal open and do not optimistically update the table.
- Runtime API 500 was caused by the backend status log persistence path, not by the frontend request body.
