# ATCS-227 Tasks

## Development Tasks

- [x] Scope product table element selectors under `.tableWrap`.
- [x] Preserve product table hover, empty-row, and row-action behavior.
- [x] Confirm no bare table-related selectors remain in `ProductTable.module.scss`.
- [x] Run targeted component tests.
- [x] Run frontend build and lint.

## Test Cases

- [x] ProductTable component tests pass.
- [x] CSS check confirms no top-level `table`, `th`, `td`, or `tr` selectors remain.
- [x] Build succeeds.
- [x] Lint succeeds.

## Modules To Modify

- `src/components/dashboard/ProductTable.module.scss`

## New Classes Or Components

- None.

## Integration Points

- Status configuration modal table styles in `Modal.module.scss`
- Product table rendering in `ProductTable.tsx`

## Result

### Changed Files

- `docs/tasks/ATCS-227/plan.md`
- `docs/tasks/ATCS-227/spec.md`
- `docs/tasks/ATCS-227/tasks.md`
- `src/components/dashboard/ProductTable.module.scss`

### Summary

- Scoped product table element selectors under `.tableWrap`.
- Removed global leakage from bare `table`, `th`, `td`, and `tr` selectors in `ProductTable.module.scss`.
- Kept product table hover, empty-row, and row-action behavior inside the product table wrapper.

### Test Results

- `rg -n "^(table|th|td|tr)([\\s:{.#]|$)|^tr:" src/components/dashboard/ProductTable.module.scss || true`
- `npm test -- --run src/components/dashboard/ProductTable.test.tsx`
- `npm run build`
- `npm run lint`

### Review Notes

- CSS-only change. The fix reduces side-effect risk by narrowing selector scope instead of changing modal table styles.
