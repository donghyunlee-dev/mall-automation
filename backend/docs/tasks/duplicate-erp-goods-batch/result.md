# duplicate-erp-goods-batch Result

## Delivered Scope

- Removed the ERP unique constraint from the goods schema and JPA mapping.
- Allowed goods registration and update to reuse an ERP goods code across multiple Godo goods rows.
- Changed the batch preparation path to compute WMS filtered stock per goods row while keeping WMS source rows shared by ERP code.
- Aligned the manual Godo force-sync documentation with the relaxed `develop` contract so the branch no longer reintroduces stale `NORMAL` / ERP guards.

## Changed Files

- `src/main/resources/schema.sql`
- `src/main/resources/postgresql/schema-postgresql.sql`
- `src/main/java/com/sfood/automation/goods/infrastructure/persistence/GoodsEntity.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCase.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCase.java`
- `src/main/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefiner.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java`
- `src/main/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJob.java`
- `src/main/java/com/sfood/automation/goods/application/usecase/ForceSyncGodoStatusUseCase.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/RegisterGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/UpdateGoodsUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`
- `src/test/java/com/sfood/automation/wms/infrastructure/scraping/StockDataRefinerTest.java`
- `src/test/java/com/sfood/automation/batch/infrastructure/job/ProductSyncBatchJobTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImplTest.java`
- `src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`
- `src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java`
- `docs/tasks/duplicate-erp-goods-batch/*`

## Key Decisions

- Decision 1:
  Keep WMS rows shared by ERP code, but key refined stock by goods id for batch execution.
- Decision 2:
  Leave the existing ERP-keyed sync API in `SyncGodoStatusUseCase` intact and add a batch-specific `executeByGoods(...)` path to minimize blast radius.
- Decision 3:
  Reset only legacy test DB schemas in test support instead of introducing a production migration path in this task.

## Open Gaps

- No production DB migration script was added here. Existing production databases need an explicit DDL change to drop the ERP unique constraint before deployment.
- Live verification covered WMS fetch/refine only. Live Godo status changes were intentionally not executed.
- `main` still carries the older force-sync guards from the merged branch history; `develop` documents the relaxed contract and should keep that line unless the code contract changes again.

## Next Steps

- Apply the production DDL change to drop the `goods.erp_goods_code` unique constraint and replace it with a non-unique index.
- Decide whether to add a repository method like `findAllByErpCode(...)` if future operator features need duplicate-ERP lookups directly.
