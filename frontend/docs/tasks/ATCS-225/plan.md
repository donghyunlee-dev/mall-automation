# ATCS-225 Plan

## Requirement Summary

Add a frontend control that lets an operator force-send a single goods item's Godo sold-out status by clicking the sale status chip in the product table.

## Business Context

Automatic batch status changes follow stock and policy conditions, but operations may need to override the current status when external constraints such as logistics center issues require stopping or resuming orders immediately.

## Scope

- Add a sale-status-chip action in the product table.
- Show a confirmation modal before sending the manual force-sync request.
- Send a single `godoGoodsCode` using the list-based `POST /api/goods/godo-status/force-sync` API.
- Reload goods after the API response and surface success or failure to the operator.

## Out Of Scope

- Bulk force-sync for multiple selected goods.
- Backend API or database changes.
- Changing batch policy or automatic sold-out calculation.

## Impacted Modules

- `src/api/goods.ts`
- `src/types/goods.ts`
- `src/components/dashboard/ProductTable.tsx`
- `src/components/dashboard/ProductTable.module.scss`
- `src/components/modals/*`
- `src/pages/DashboardPage.tsx`
- Existing component and API tests.

## Architecture Considerations

- Keep the API client list-based to support future bulk force-sync.
- Reuse the existing modal confirmation pattern from Godo sync block changes.
- Do not optimistically mutate table state; reload goods only after a response is received.
- Keep eligibility checks aligned with backend validation: `NORMAL`, ERP sync `Y`, Godo sync `Y`, and Godo sync block `N`.

## Risk Analysis

- A manual force-sync is an external side-effect against Godo, so accidental clicks must be protected by a confirmation modal.
- Frontend eligibility can become stale between click and confirm; non-`SUCCESS` item results must be treated as failures.
- The sale status chip is already compact, so clickable affordance must be visible without disrupting the current table layout.
