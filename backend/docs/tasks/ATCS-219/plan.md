# ATCS-219 Plan

## Requirement Summary

- synchronize existing goods criteria when `CommonStatusConfig` is updated
- ensure scope-specific `availableQty` and `safetyRate` values stay aligned with the latest config

## Business Context

- goods status transitions depend on criteria stored on each goods row, not on live reads from `common_status_config`
- if config changes do not propagate, batch judgments continue using stale thresholds and operators see inconsistent behavior

## Scope

- update the config change use case to propagate `outOfStockQty` and `safetyRate` to goods in the same scope
- keep scope isolation between `GENERAL` and `EXCLUSIVE`
- add focused tests for config update synchronization behavior
- document the task and validation results

## Out Of Scope

- changing how `on_sale_qty` is used in transition policy
- altering product registration defaults beyond keeping them consistent with synced config
- changing batch orchestration, WMS scraping, or Godo API flows

## Impacted Modules

- `src/main/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCase.java`
- `src/main/java/com/sfood/automation/goods/domain/port/GoodsRepository.java`
- `src/main/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImpl.java`
- `src/test/java/com/sfood/automation/goods/application/usecase/UpdateCommonStatusConfigUseCaseTest.java`
- `src/test/java/com/sfood/automation/goods/infrastructure/persistence/GoodsRepositoryImplTest.java`

## Architecture Considerations

- keep the synchronization in the application use case that owns config updates
- depend on the `GoodsRepository` port instead of coupling the use case to persistence details
- update persisted goods criteria in place without changing transition policy contracts

## Risk Analysis

- syncing the wrong scope would silently change unrelated goods thresholds
- excluding hidden goods would leave stale criteria that can resurface when goods are restored later
- missing regression coverage could allow future config updates to drift from goods criteria again
