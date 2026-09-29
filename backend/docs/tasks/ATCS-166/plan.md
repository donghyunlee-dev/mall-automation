# ATCS-166 Plan

## Requirement Summary

- change `GET /api/config/status` from a single-object lookup to a list response containing all status configs
- remove the `scope` query parameter from the read API
- keep the existing update API behavior unchanged

## Business Context

- the configuration popup in the web page loads all sales-status thresholds at once before editing
- per-scope lookup is unused, so the current single-object contract adds unnecessary client logic
- the read API should reflect the actual UI flow and always expose both configured scopes

## Scope

- update the common status config read use case and controller to return a list
- load both persisted configs from the repository in deterministic order
- update API docs and automated tests for the new contract

## Out Of Scope

- changes to the config update API
- schema or seed-data changes
- frontend updates outside the backend API contract

## Impacted Modules

- `goods.presentation`
- `goods.application.usecase`
- `goods.domain.port`
- `goods.infrastructure.persistence`
- controller and use case tests
- API and task documentation

## Architecture Considerations

- keep the controller thin and move list assembly through the existing use case layer
- preserve the existing repository port abstraction by adding a list query rather than coupling the use case to JPA
- return configs in stable repository order so the client gets predictable `GENERAL`, `EXCLUSIVE` data

## Risk Analysis

- changing the repository port requires updating all fake repositories used by goods-related tests
- list ordering must be deterministic to avoid brittle UI or test assumptions
- query-parameter removal must not affect the update endpoint contract
