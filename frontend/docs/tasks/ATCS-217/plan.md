# ATCS-217 Plan

## Requirement Summary

- Expose Common Status Config on the dashboard so operators can view and update sale-state thresholds without going through DB or API tools.
- Use the existing backend APIs to load and save `GENERAL` and `EXCLUSIVE` config values.
- Keep `onSaleQty` read-only in the UI and derive it as `outOfStockQty + 1`.

## Business Context

- The current product automation behavior depends on global threshold data stored in `common_status_config`.
- Operators currently cannot verify or change those values from the product management screen.
- The feature should make the policy visible and editable while preserving existing product CRUD and Excel workflows.

## Scope

- Add frontend types and API functions for common status config.
- Add dashboard-level state management for config loading and saving.
- Add a modal entry point on the product list card to view and edit config values.
- Show read-only reference text in product add/edit modals using the loaded config.

## Out Of Scope

- Backend API or schema changes.
- New routes or standalone settings pages.
- Changing product-level `availableQty` and `safetyRate` editing behavior.

## Impacted Modules

- `src/types/`
- `src/api/`
- `src/hooks/`
- `src/pages/DashboardPage.tsx`
- `src/components/modals/`

## Architecture Considerations

- Keep API calls in `src/api/`.
- Centralize config fetch/save state in a dedicated hook owned by the dashboard.
- Pass config down to product modals as props so they reuse already-fetched browser state.

## Risk Analysis

- Save behavior could drift from backend expectations if the derived `onSaleQty` rule is not enforced consistently.
- UI state can become stale after save unless the dashboard-level state is updated immediately.
- Added modal and props wiring must not regress existing product add/edit flows.
