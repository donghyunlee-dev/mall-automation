# ATCS-224 Plan

## Requirement Summary

- add an authenticated backend API that force-sends sold-out status updates to Godo for selected goods
- bypass WMS quantity, valid-percent, zone, and lot checks for this manual path
- allow force sync only for goods that are `NORMAL`, ERP-synced, Godo-synced, and not blocked from Godo sync

## Business Context

- operators need an explicit manual control path when the Godo status must be updated immediately
- the batch path remains responsible for automatic WMS-based decisions and must not change behavior

## Scope

- request and response DTOs for force Godo status sync
- controller endpoint for authenticated manual sync requests
- separate use case for eligibility validation, Godo API call, local goods update, and status log persistence
- targeted tests, API spec update, and iteration documents

## Out Of Scope

- batch policy changes
- WMS scraping or stock aggregation changes
- UI changes
- additional audit tables or schema changes outside the existing goods and status-log flow

## Impacted Modules

- `goods.presentation`
- `goods.application.dto`
- `goods.application.usecase`
- `goods.domain.port`
- API and iteration documentation

## Architecture Considerations

- keep manual force sync in a separate use case from `SyncGodoStatusUseCase`
- reuse `GodoApiPort` and `GoodsStatusLogRepository`
- keep controller thin and pass session user into the use case

## Risk Analysis

- if eligibility rules are too loose, invalid goods could be sent to Godo
- if the manual path reuses batch policy code, scope can expand and regress automatic sync behavior
- if per-item results are not explicit, operators cannot distinguish skipped, missing, and failed goods
