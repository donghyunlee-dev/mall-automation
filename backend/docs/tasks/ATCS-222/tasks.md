# ATCS-222 Tasks

## Development Tasks

- add the `godo_sync_blocked_yn` column to schema and update the documented goods data spec
- extend goods entity, domain model, repository mapping, and response mapping with the new flag
- add a dedicated request DTO, use case, and controller endpoint to update the flag
- prevent enabling the flag for non-`NORMAL` goods
- skip blocked goods in the Godo sync batch flow

## Test Cases To Implement

- controller test for the new patch endpoint success path
- controller test for invalid `blockedYn` input
- use case test for enabling the block on `NORMAL` goods
- use case test for rejecting enable on non-`NORMAL` goods
- sync use case test for skipping blocked goods before API transmission
- repository test for persisting and reading the new column

## Modules To Modify

- `src/main/resources/schema.sql`
- `src/main/java/com/sfood/automation/goods/**`
- `src/main/java/com/sfood/automation/common/exception/**`
- `src/test/java/com/sfood/automation/goods/**`
- `docs/product/data-spec.md`

## New Classes Or Components

- request DTO for the block-setting API
- use case for updating the block flag
- business exception for invalid goods transmission-block activation

## Integration Points

- frontend goods detail page calls the new patch API
- product sync batch checks the flag before `GodoApiPort.updateSoldOutStatus(...)`

## Result

- Changed files:
  - `src/main/resources/schema.sql`
  - `src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigration.java`
  - `src/main/java/com/sfood/automation/common/bootstrap/SqliteSchemaMigrationDependencyConfig.java`
  - `src/main/java/com/sfood/automation/common/exception/ErrorCode.java`
  - `src/main/java/com/sfood/automation/common/exception/GoodsSyncBlockValidationException.java`
  - `src/main/java/com/sfood/automation/goods/application/dto/GoodsResponse.java`
  - `src/main/java/com/sfood/automation/goods/application/dto/UpdateGodoSyncBlockRequest.java`
  - `src/main/java/com/sfood/automation/goods/application/usecase/GoodsMapper.java`
  - `src/main/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCase.java`
  - `src/main/java/com/sfood/automation/goods/application/usecase/UpdateGodoSyncBlockUseCase.java`
  - `src/main/java/com/sfood/automation/goods/domain/model/Goods.java`
  - `src/main/java/com/sfood/automation/goods/infrastructure/persistence/GoodsEntity.java`
  - `src/main/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImpl.java`
  - `src/main/java/com/sfood/automation/goods/presentation/GoodsController.java`
  - `src/test/java/com/sfood/automation/goods/application/usecase/UpdateGodoSyncBlockUseCaseTest.java`
  - `src/test/java/com/sfood/automation/goods/application/usecase/SyncGodoStatusUseCaseTest.java`
  - `src/test/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImplTest.java`
  - `src/test/java/com/sfood/automation/goods/presentation/GoodsControllerTest.java`
  - `src/test/java/com/sfood/automation/goods/presentation/GoodsListControllerTest.java`
  - `src/test/java/com/sfood/automation/goods/presentation/GoodsStatusLogControllerTest.java`
  - `src/test/java/com/sfood/automation/support/SharedSqliteTestDatabase.java`
  - `docs/product/data-spec.md`
- Summary:
  - added a persisted `godo_sync_blocked_yn` flag and exposed it through goods responses
  - added `PATCH /api/goods/{id}/godo-sync-block` with `NORMAL`-only enable validation
  - skipped blocked goods before any Godo API call in the sync use case
  - kept shared SQLite test DB compatible by backfilling the new column when the file already exists
- Test results:
  - targeted goods tests: PASS
  - repository-wide `./gradlew test`: PASS
- Review notes:
  - the full-suite failures during development were wiring regressions in `GoodsController` slice tests and were resolved by adding the new mocked use case
