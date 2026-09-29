# ATCS-233 Tasks

## Implementation Tasks

- [x] Exclude `HIDE` goods from `findAllErpSyncNGoods()`.
- [x] Add a defensive skip in batch ERP verification for hidden goods.

## Test Tasks

- [x] Add repository coverage for hidden goods exclusion from ERP verification candidates.
- [x] Add batch job coverage for hidden goods not being restored or saved during ERP verification.

## Documentation Tasks

- [x] Record validation output in `test-result.md`.
- [x] Record delivered scope and changed files in `result.md`.

## Progress Notes

- Jira issue reviewed and scope limited to the batch ERP verification defect.
- Existing list query already excludes `HIDE`; defect path is the ERP verification candidate flow.
- Repository filtering and batch defensive guard were both applied to keep hidden goods protected.

## Review Notes

- Keep manual restore behavior in `RegisterGoodsUseCase` unchanged.
- Regression coverage now exists for both repository selection and batch ERP verification behavior.
