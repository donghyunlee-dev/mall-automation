# ATCS-170 Plan

## Requirement Summary

- fix the WMS stock-refinement flow so inventory rows are aggregated by item after collection
- ensure multiple `lot_no` rows for the same ERP item are combined into one refined stock result with summed quantity
- use the refined aggregated stock as the basis for downstream sold-out and on-sale decisions

## Business Context

- the WMS source manages inventory by `lot_no`, so one ERP item can appear across many rows
- the batch is expected to reduce raw WMS rows into item-level inventory before deciding whether a goods item should sync to GodoMall
- without item-level refinement, the batch summary and intermediate data do not match the Jira requirement for producing condensed stock data after collection

## Scope

- aggregate registered WMS stock rows into item-level refined stock data
- pass the aggregated refinement result through the batch job into the goods sync use case
- add focused regression tests and task documentation for this Jira issue

## Out Of Scope

- changes to WMS transport, login, or payload parsing
- changes to the sold-out policy thresholds themselves
- scheduler, branch, commit, push, or pull request automation

## Impacted Modules

- `batch.infrastructure.job`
- `wms.infrastructure.scraping`
- `goods.application.usecase`
- focused unit tests under `src/test/java`
- Jira task documentation under `backend/docs/tasks/ATCS-170`

## Architecture Considerations

- keep raw WMS collection separate from refinement so the batch job still owns orchestration
- move item-level aggregation into the refinement step because that is the first point where business-relevant stock data is derived from the raw WMS feed
- keep `SyncGodoStatusUseCase` able to consume aggregated stock directly without rebuilding the same intermediate view in the batch flow

## Risk Analysis

- aggregation must preserve policy inputs such as minimum valid percent and lot existence
- collapsing rows too early must not break ERP verification, which still needs the raw WMS item-code set
- summary counts and downstream tests must be updated so the refined row count reflects aggregated item-level data rather than raw filtered rows
