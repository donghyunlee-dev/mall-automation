# duplicate-erp-goods-batch Plan

## Summary

- Remove ERP uniqueness constraints from the goods persistence model and application validation.
- Replace the batch's single-goods ERP map with a goods-aware refinement flow that computes filtered stock per goods row while reusing the shared WMS data set.
- Keep the Godo sync loop goods-centric so each goods row still produces its own API decision and call.

## Impacted Modules

- `src/main/resources/schema.sql`
- `src/main/resources/postgresql/schema-postgresql.sql`
- `src/main/java/com/sfood/automation/goods/infrastructure/persistence`
- `src/main/java/com/sfood/automation/goods/application/usecase`
- `src/main/java/com/sfood/automation/wms/infrastructure/scraping`
- `src/main/java/com/sfood/automation/batch/infrastructure/job`
- related unit and flow tests under `src/test/java`

## Architecture Approach

- Preserve the existing layering:
  - persistence constraints in infrastructure
  - duplicate ERP validation changes in goods use cases
  - WMS row filtering and aggregation in the scraping module
  - orchestration in the batch job and sync use case
- Avoid coupling batch logic directly to JPA details; keep goods-based evaluation in existing domain/application paths.

## Data And API Impact

- Database schema changes:
  - remove the unique constraint and unique index on `goods.erp_goods_code`
- Repository/use case changes:
  - stop using single-result ERP uniqueness checks as a validation gate
- Internal batch contract changes:
  - refinement input/output may need to become goods-aware instead of ERP-to-single-goods aware
- External API behavior:
  - no request contract change

## Validation Plan

- Update unit tests for registration and update duplicate ERP acceptance.
- Add targeted batch/refiner tests for duplicate ERP goods with different `safetyRate` and `availableQty`.
- Run focused tests for goods use cases, refiner, sync use case, and batch job.
- Run the full Gradle test suite after targeted tests are green.
- If the environment permits, run a controlled local verification with a single live WMS-backed goods row as requested.

## Risks

- Risk 1:
  - A partial refactor may keep one single-ERP assumption in the batch path and silently apply the wrong `safetyRate`.
- Risk 2:
  - Schema changes without matching JPA and use case changes could leave registration/update inconsistent with runtime behavior.
- Risk 3:
  - Live WMS verification may be constrained by environment access or existing local DB contents.

## Rollback Considerations

- The change is isolated to goods uniqueness and batch refinement behavior.
- If an issue is found, revert the feature branch or restore the ERP unique constraint and previous batch refinement path together; partial rollback is unsafe.
