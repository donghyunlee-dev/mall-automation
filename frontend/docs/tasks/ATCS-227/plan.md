# ATCS-227 Plan

## Requirement Summary

Fix the layout regression where product table CSS leaks into the status configuration table.

## Business Context

Operators use the status configuration modal to manage sale-status thresholds. A product-list-only table layout change should not alter the modal's configuration table.

## Scope

- Scope product table element selectors to the product table wrapper.
- Preserve the current product table layout, badges, and row actions.
- Verify the status configuration table no longer receives product table sizing or cell styles.

## Out Of Scope

- Redesigning the status configuration modal.
- Changing product table column percentages or business behavior.
- Modifying backend code.

## Impacted Modules

- `src/components/dashboard/ProductTable.module.scss`
- Product table component tests and frontend build/lint validation.

## Architecture Considerations

- CSS Modules do not make bare element selectors component-local. Selectors such as `table`, `th`, `td`, and `tr:hover td` must be anchored to a local class.
- The safest fix is to nest product table element rules under `.tableWrap` so the compiled selector includes the scoped wrapper class.

## Risk Analysis

- The product table could regress if hover, empty-row, or row-action selectors are not scoped with the same behavior.
- Other tables should improve because the product table rules stop leaking globally.
