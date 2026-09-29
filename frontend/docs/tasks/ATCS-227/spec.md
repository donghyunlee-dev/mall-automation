# ATCS-227 Spec

## Feature Specification

- Product table styles must apply only inside `ProductTable`'s `.tableWrap`.
- Status configuration table styles must remain controlled by `Modal.module.scss`.
- No global `table`, `th`, `td`, or `tr` selectors should remain in `ProductTable.module.scss`.

## API Changes

- None.

## Database Changes

- None.

## Domain Model Changes

- None.

## Validation Rules

- Product table layout rules must be scoped under `.tableWrap`.
- Empty-row and row-hover behavior must remain scoped to the product table.
- Status configuration modal must not inherit product table `min-width: 960px` or `table-layout: fixed`.

## Edge Cases

- Product table empty rows must keep their fixed height and borders.
- Row action buttons must still reveal on product table row hover.
- Touch devices must still show row actions.

## Exception Scenarios

- If a bare `table`, `th`, `td`, or `tr` selector remains in `ProductTable.module.scss`, the fix is incomplete.

## Acceptance Criteria Mapping

- Happy path: status configuration table renders with modal-specific styles only.
- Happy path: product table layout remains unchanged.
- Failure path: global product table selectors are absent after the change.
