# ATCS-179 Plan

## Requirement Summary

- expand the single goods detail API response so omitted `goods` table fields are returned
- keep the existing endpoint and behavior intact while adding the missing response fields

## Business Context

- the goods detail screen needs the full goods record to support operational review without additional API calls
- the Jira ticket specifically targets the single-item query API, not the goods list API

## Scope

- update the goods detail response DTO and mapper
- add or update focused tests for the expanded response contract
- update API documentation for `GET /api/goods/{id}`

## Out Of Scope

- changes to the goods list API response
- changes to persistence, schema, or batch behavior
- unrelated cleanup in already modified goods modules

## Impacted Modules

- `goods.application.dto`
- `goods.application.usecase`
- `goods.presentation`
- goods-related test suites
- product API documentation

## Architecture Considerations

- keep controller, use case, and repository responsibilities unchanged
- preserve interface-first wiring by limiting changes to DTO mapping and presentation contract

## Risk Analysis

- widening the API contract requires synchronized updates in controller tests and API docs
- the task must stay isolated from unrelated goods list and batch changes already in progress elsewhere
