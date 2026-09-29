# ATCS-175 Plan

## Requirement Summary

- extend the goods update API so callers can modify `available_qty` and `safety_rate`
- persist the updated criteria values to the `goods` table when a product is edited
- update the API documentation to reflect the request contract change

## Business Context

- goods registration initializes criteria from the common status config, but operators also need to tune those values per goods item after registration
- the current update flow only allows name, ERP code, and mall scope changes, so individual criteria cannot be adjusted
- the Jira ticket requires the update API and docs to support those per-item adjustments

## Scope

- add `availableQty` and `safetyRate` to the goods update request DTO
- update the goods update use case so edited goods store the request criteria values
- add focused controller and use case coverage for the extended update request
- update product API documentation and Jira task docs for the new request fields

## Out Of Scope

- goods registration request changes
- common status config behavior changes
- schema changes to the `goods` table
- frontend code changes
- git branch, commit, push, or pull request automation

## Impacted Modules

- `goods.application.dto`
- `goods.application.usecase`
- `goods.presentation`
- product documentation under `docs/product`
- Jira task documentation under `docs/tasks/ATCS-175`

## Architecture Considerations

- keep the change inside the existing goods update API path and use case
- preserve the existing repository and response contract while extending the request payload
- avoid reloading common config during updates because the Jira requirement is to store explicit per-item criteria

## Risk Analysis

- missing validation on the new numeric fields can allow invalid criteria values into the DB
- changing the update request contract requires tests and docs to be updated together or consumers will drift
- removing the config-driven reset in updates changes existing behavior, so tests must prove the request values are persisted
